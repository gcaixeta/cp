package dev.gustavorosa.cpsystem.service;

import dev.gustavorosa.cpsystem.api.request.CreatePaymentGroupRequest;
import dev.gustavorosa.cpsystem.boleto.model.BankType;
import dev.gustavorosa.cpsystem.boleto.service.BoletoService;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.model.factory.PaymentGroupFactory;
import dev.gustavorosa.cpsystem.repository.PaymentGroupRepository;
import dev.gustavorosa.cpsystem.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
