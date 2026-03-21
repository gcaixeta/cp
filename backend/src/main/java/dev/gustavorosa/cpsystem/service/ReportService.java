package dev.gustavorosa.cpsystem.service;

import dev.gustavorosa.cpsystem.api.response.MonthlyReportData;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
import dev.gustavorosa.cpsystem.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PdfReportGenerator pdfReportGenerator;

    public byte[] generateMonthlyReport(Long clientId, int month, int year) {
        log.info("Generating monthly report for client {} - {}/{}", clientId, month, year);

        Client client = clientService.findClientById(clientId);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Payments with due date in the month (for status breakdown)
        List<Payment> dueInMonth = paymentRepository.findByClientIdAndDueDateBetween(clientId, startDate, endDate);

        // Payments actually paid in the month (for total received)
        List<Payment> paidInMonth = paymentRepository.findByClientIdAndPaymentDateBetween(clientId, startDate, endDate);

        MonthlyReportData reportData = buildReportData(client, month, year, dueInMonth, paidInMonth);
        return pdfReportGenerator.generate(reportData);
    }

    private MonthlyReportData buildReportData(Client client, int month, int year,
                                               List<Payment> dueInMonth, List<Payment> paidInMonth) {
        int total = dueInMonth.size();

        int paidOnTime = 0;
        int paidLate = 0;
        int pending = 0;
        int overdue = 0;

        for (Payment p : dueInMonth) {
            switch (p.getPaymentStatus()) {
                case PAID -> paidOnTime++;
                case PAID_LATE -> paidLate++;
                case PENDING -> pending++;
                case OVERDUE -> overdue++;
                default -> {}
            }
        }

        double paidOnTimePct = total > 0 ? (paidOnTime * 100.0) / total : 0;
        double paidLatePct = total > 0 ? (paidLate * 100.0) / total : 0;

        // Total received = sum of originalValue (or overdueValue if PAID_LATE) for payments paid in the month
        BigDecimal totalReceived = paidInMonth.stream()
                .map(p -> {
                    if (p.getPaymentStatus() == PaymentStatus.PAID_LATE && p.getOverdueValue() != null) {
                        return p.getOverdueValue();
                    }
                    return p.getOriginalValue();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Total outstanding = sum of pending + overdue payments' values
        BigDecimal totalOutstanding = dueInMonth.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PENDING || p.getPaymentStatus() == PaymentStatus.OVERDUE)
                .map(p -> {
                    if (p.getPaymentStatus() == PaymentStatus.OVERDUE && p.getOverdueValue() != null) {
                        return p.getOverdueValue();
                    }
                    return p.getOriginalValue();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Average days difference (positive = early, negative = late)
        List<Payment> paidPayments = dueInMonth.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID || p.getPaymentStatus() == PaymentStatus.PAID_LATE)
                .filter(p -> p.getPaymentDate() != null)
                .toList();

        double avgDays = 0;
        if (!paidPayments.isEmpty()) {
            long totalDaysDiff = paidPayments.stream()
                    .mapToLong(p -> ChronoUnit.DAYS.between(p.getPaymentDate(), p.getDueDate()))
                    .sum();
            avgDays = (double) totalDaysDiff / paidPayments.size();
        }

        // Group breakdowns
        Map<Long, List<Payment>> byGroup = dueInMonth.stream()
                .collect(Collectors.groupingBy(p -> p.getPaymentGroup().getId()));

        // Also collect paid-in-month payments by group for received calculation
        Map<Long, List<Payment>> paidByGroup = paidInMonth.stream()
                .collect(Collectors.groupingBy(p -> p.getPaymentGroup().getId()));

        List<MonthlyReportData.GroupBreakdown> breakdowns = byGroup.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> {
                    Long groupId = entry.getKey();
                    List<Payment> payments = entry.getValue();
                    String groupName = payments.getFirst().getPaymentGroup().getGroupName();

                    int gPaidOnTime = 0, gPaidLate = 0, gPending = 0, gOverdue = 0;
                    for (Payment p : payments) {
                        switch (p.getPaymentStatus()) {
                            case PAID -> gPaidOnTime++;
                            case PAID_LATE -> gPaidLate++;
                            case PENDING -> gPending++;
                            case OVERDUE -> gOverdue++;
                            default -> {}
                        }
                    }

                    BigDecimal gReceived = paidByGroup.getOrDefault(groupId, List.of()).stream()
                            .map(p -> {
                                if (p.getPaymentStatus() == PaymentStatus.PAID_LATE && p.getOverdueValue() != null) {
                                    return p.getOverdueValue();
                                }
                                return p.getOriginalValue();
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal gOutstanding = payments.stream()
                            .filter(p -> p.getPaymentStatus() == PaymentStatus.PENDING || p.getPaymentStatus() == PaymentStatus.OVERDUE)
                            .map(p -> {
                                if (p.getPaymentStatus() == PaymentStatus.OVERDUE && p.getOverdueValue() != null) {
                                    return p.getOverdueValue();
                                }
                                return p.getOriginalValue();
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return new MonthlyReportData.GroupBreakdown(
                            groupId, groupName, payments.size(),
                            gPaidOnTime, gPaidLate, gPending, gOverdue,
                            gReceived, gOutstanding
                    );
                })
                .toList();

        // Sort all payments by due date for the table
        List<Payment> sortedPayments = dueInMonth.stream()
                .sorted(Comparator.comparing(Payment::getDueDate).thenComparing(p -> p.getPaymentGroup().getId()))
                .toList();

        return new MonthlyReportData(
                client, month, year, total,
                paidOnTime, paidOnTimePct,
                paidLate, paidLatePct,
                pending, overdue,
                totalReceived, totalOutstanding,
                avgDays, breakdowns, sortedPayments
        );
    }
}
