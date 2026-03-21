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
        int paidOnTime,
        double paidOnTimePercentage,
        int paidLate,
        double paidLatePercentage,
        int pending,
        int overdue,
        BigDecimal totalReceived,
        BigDecimal totalOutstanding,
        double averageDaysDifference,
        List<GroupBreakdown> groupBreakdowns,
        List<Payment> allPayments
) {
    public record GroupBreakdown(
            Long groupId,
            String groupName,
            int totalPayments,
            int paidOnTime,
            int paidLate,
            int pending,
            int overdue,
            BigDecimal received,
            BigDecimal outstanding
    ) {}
}
