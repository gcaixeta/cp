package dev.gustavorosa.cpsystem.api.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentGroupResponse(
        Long id,
        String groupName,
        String clientName,
        Long clientId,
        String payerDocument,
        String payerPhone,
        int totalInstallments,
        int paidInstallments,
        BigDecimal lateFeeRate,
        BigDecimal monthlyInterestRate,
        BigDecimal monthlyValue,
        BigDecimal totalPaid,
        BigDecimal totalRemaining,
        LocalDate creationDate,
        String observation
) {
}
