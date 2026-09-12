package dev.gustavorosa.cpsystem.model.factory;

import dev.gustavorosa.cpsystem.utils.NameUtils;
import dev.gustavorosa.cpsystem.api.request.CreatePaymentGroupRequest;
import dev.gustavorosa.cpsystem.exception.NoClientFoundException;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
import dev.gustavorosa.cpsystem.repository.ClientRepository;
import dev.gustavorosa.cpsystem.repository.PaymentGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class PaymentGroupFactory {
    @Autowired
    private PaymentGroupRepository paymentGroupRepository;
    @Autowired
    private ClientRepository clientRepository;

    public PaymentGroup buildPaymentGroup(CreatePaymentGroupRequest dto) {
        Client client = clientRepository.findById(dto.clientId())
                .orElseThrow(() -> new NoClientFoundException("Client not found with id: " + dto.clientId()));
        
        return PaymentGroup.builder()
                .client(client)
                .groupName(buildGroupName(dto))
                .payerDocument(dto.payerDocument())
                .payerPhone(dto.payerPhone())
                .totalInstallments(dto.totalInstallments())
                .lateFeeRate(dto.lateFeeRate() != null ? dto.lateFeeRate() : client.getLateFeeRate())
                .monthlyInterestRate(dto.monthlyInterestRate() != null ? dto.monthlyInterestRate() : client.getMonthlyInterestRate()   )
                .creationDate(LocalDate.now())
                .observation(dto.observation())
                .build();
    }

    private String buildGroupName(CreatePaymentGroupRequest dto) {
        long existingGroupsCount = paymentGroupRepository.countByPayerDocument(dto.payerDocument());
        long nextGroupNumber = existingGroupsCount + 1;
        return dto.payerDocument() + "-" + nextGroupNumber;
    }

    public List<Payment> buildPaymentList(PaymentGroup newPaymentGroup,CreatePaymentGroupRequest dto) {
        List<Payment> payments = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 1; i <= newPaymentGroup.getTotalInstallments(); i++) {
            LocalDate dueDate = dto.firstInstallmentDueDate().plusMonths((i-1));
            payments.add(Payment.builder()
                .client(newPaymentGroup.getClient())
                .paymentGroup(newPaymentGroup)
                .payerName(NameUtils.toTitleCase(dto.payerName()))
                .payerDocument(newPaymentGroup.getPayerDocument())
                .installmentNumber(i)
                .totalInstallments(newPaymentGroup.getTotalInstallments())
                .dueDate(dueDate)
                .originalValue(dto.monthlyValue())
                .paymentStatus(calculatePaymentStatus(dueDate, today))
                .observation(dto.observation())
                .build());
        }
        return payments;
    }

    private PaymentStatus calculatePaymentStatus(LocalDate dueDate, LocalDate today) {
        if (dueDate.isBefore(today)) {
            return PaymentStatus.OVERDUE;
        }
        return PaymentStatus.PENDING;
    }
}
