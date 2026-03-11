package dev.gustavorosa.cpsystem.service;

import dev.gustavorosa.cpsystem.api.request.CreatePaymentGroupRequest;
import dev.gustavorosa.cpsystem.api.response.PaymentGroupResponse;
import dev.gustavorosa.cpsystem.boleto.model.BankType;
import dev.gustavorosa.cpsystem.boleto.service.BoletoService;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
import dev.gustavorosa.cpsystem.model.factory.PaymentGroupFactory;
import dev.gustavorosa.cpsystem.repository.PaymentGroupRepository;
import dev.gustavorosa.cpsystem.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class PaymentGroupService {

    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private PaymentGroupRepository paymentGroupRepository;
    @Autowired
    private PaymentGroupFactory paymentGroupFactory;
    @Autowired
    private BoletoService boletoService;

    public List<PaymentGroupResponse> findAllPaymentGroups(Long clientId) {
        List<PaymentGroup> groups;
        if (clientId != null) {
            groups = paymentGroupRepository.findByClientId(clientId);
        } else {
            groups = paymentGroupRepository.findAll();
        }

        return groups.stream().map(this::toResponse).toList();
    }

    private PaymentGroupResponse toResponse(PaymentGroup group) {
        List<Payment> payments = paymentRepository.findByPaymentGroupId(group.getId());

        int paidInstallments = (int) payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID || p.getPaymentStatus() == PaymentStatus.PAID_LATE)
                .count();

        BigDecimal monthlyValue = payments.isEmpty() ? BigDecimal.ZERO : payments.get(0).getOriginalValue();

        BigDecimal totalPaid = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID || p.getPaymentStatus() == PaymentStatus.PAID_LATE)
                .map(p -> p.getOverdueValue() != null ? p.getOverdueValue() : p.getOriginalValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRemaining = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PENDING || p.getPaymentStatus() == PaymentStatus.OVERDUE)
                .map(p -> p.getOverdueValue() != null ? p.getOverdueValue() : p.getOriginalValue())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PaymentGroupResponse(
                group.getId(),
                group.getGroupName(),
                group.getClient().getName(),
                group.getClient().getId(),
                group.getPayerDocument(),
                group.getPayerPhone(),
                group.getTotalInstallments(),
                paidInstallments,
                group.getLateFeeRate(),
                group.getMonthlyInterestRate(),
                monthlyValue,
                totalPaid,
                totalRemaining,
                group.getCreationDate(),
                group.getObservation()
        );
    }

    @Transactional
    public void createPaymentGroup(CreatePaymentGroupRequest request) {
        PaymentGroup newPaymentGroup = paymentGroupFactory.buildPaymentGroup(request);
        paymentGroupRepository.save(newPaymentGroup);

        List<Payment> paymentList = paymentGroupFactory.buildPaymentList(newPaymentGroup, request);
        List<Payment> savedPayments = paymentRepository.saveAll(paymentList);

        // NOVO: Gerar boletos se solicitado
        if (Boolean.TRUE.equals(request.generateBoletos())) {
            BankType bankType = determineBankType(newPaymentGroup.getClient());
            
            log.info("Gerando boletos para {} pagamentos do grupo {}", 
                savedPayments.size(), newPaymentGroup.getId());
            
            savedPayments.forEach(payment -> {
                try {
                    boletoService.generateBoletoForPayment(payment.getId(), bankType);
                    log.info("Boleto gerado com sucesso para payment {}", payment.getId());
                } catch (Exception e) {
                    log.error("Erro ao gerar boleto para payment {}", payment.getId(), e);
                    // Continua sem falhar toda a operação
                }
            });
        }
    }

    private BankType determineBankType(Client client) {
        // Lógica para determinar banco baseado no client.bank
        // Por enquanto, retornar INTER como padrão
        if (client.getBank() != null) {
            String bankName = client.getBank().toUpperCase();
            if (bankName.contains("INTER")) {
                return BankType.INTER;
            } else if (bankName.contains("ITAU") || bankName.contains("ITAÚ")) {
                return BankType.ITAU;
            } else if (bankName.contains("BRADESCO")) {
                return BankType.BRADESCO;
            } else if (bankName.contains("BRASIL")) {
                return BankType.BANCO_DO_BRASIL;
            }
        }
        
        // Padrão: Banco Inter
        return BankType.INTER;
    }
}
