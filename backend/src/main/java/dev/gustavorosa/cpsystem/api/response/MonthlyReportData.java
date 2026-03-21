package dev.gustavorosa.cpsystem.api.response;

import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyReportData(
        Client client,
        int month,
        int year,
        int totalPayments,
        int paidEarly,
        double paidEarlyPercentage,
        int paidOnDueDate,
        double paidOnDueDatePercentage,
        int paidLate,
        double paidLatePercentage,
        int pending,
        int overdue,
        BigDecimal totalReceived,
        BigDecimal totalOutstanding,
        double averageDaysLate,
        List<Payment> allPayments
) {
}
