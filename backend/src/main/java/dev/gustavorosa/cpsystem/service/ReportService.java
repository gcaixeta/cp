package dev.gustavorosa.cpsystem.service;

import dev.gustavorosa.cpsystem.api.response.MonthlyReportData;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
import dev.gustavorosa.cpsystem.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

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

        int paidEarly = 0;
        int paidOnDueDate = 0;
        int paidLate = 0;
        int pending = 0;
        int overdue = 0;

        for (Payment p : dueInMonth) {
            switch (p.getPaymentStatus()) {
                case PAID -> {
                    if (p.getPaymentDate() != null && p.getPaymentDate().isBefore(p.getDueDate())) {
                        paidEarly++;
                    } else {
                        paidOnDueDate++;
                    }
                }
                case PAID_LATE -> paidLate++;
                case PENDING -> pending++;
                case OVERDUE -> overdue++;
                default -> {}
            }
        }

        double paidEarlyPct = total > 0 ? (paidEarly * 100.0) / total : 0;
        double paidOnDueDatePct = total > 0 ? (paidOnDueDate * 100.0) / total : 0;
        double paidLatePct = total > 0 ? (paidLate * 100.0) / total : 0;

        // Total received = sum of originalValue (or valor com multa/juros se PAID_LATE) para pagamentos do mês.
        // Soma cada parcela com precisão total e arredonda só o total, para não divergir do valor
        // combinado do banco por causa da soma de centavos já arredondados individualmente.
        BigDecimal totalReceived = paidInMonth.stream()
                .map(p -> {
                    if (p.getPaymentStatus() == PaymentStatus.PAID_LATE && p.getPaymentDate() != null) {
                        long daysLate = ChronoUnit.DAYS.between(p.getDueDate(), p.getPaymentDate());
                        return p.getOriginalValue().add(surcharge(p, daysLate));
                    }
                    return p.getOriginalValue();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Total outstanding = sum of pending + overdue payments' values (mesma lógica de precisão acima)
        LocalDate today = LocalDate.now();
        BigDecimal totalOutstanding = dueInMonth.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PENDING || p.getPaymentStatus() == PaymentStatus.OVERDUE)
                .map(p -> {
                    if (p.getPaymentStatus() == PaymentStatus.OVERDUE) {
                        long daysOverdue = ChronoUnit.DAYS.between(p.getDueDate(), today);
                        return p.getOriginalValue().add(surcharge(p, daysOverdue));
                    }
                    return p.getOriginalValue();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Average days late for PAID_LATE payments only (positive = days overdue)
        List<Payment> latePaidPayments = dueInMonth.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PAID_LATE)
                .filter(p -> p.getPaymentDate() != null)
                .toList();

        double avgDaysLate = latePaidPayments.isEmpty() ? 0.0 :
                latePaidPayments.stream()
                        .mapToLong(p -> ChronoUnit.DAYS.between(p.getDueDate(), p.getPaymentDate()))
                        .average().orElse(0.0);

        // Sort all payments by due date for the table
        List<Payment> sortedPayments = dueInMonth.stream()
                .sorted(Comparator.comparing(Payment::getDueDate).thenComparing(p -> p.getPaymentGroup().getId()))
                .toList();

        return new MonthlyReportData(
                client, month, year, total,
                paidEarly, paidEarlyPct,
                paidOnDueDate, paidOnDueDatePct,
                paidLate, paidLatePct,
                pending, overdue,
                totalReceived, totalOutstanding,
                avgDaysLate, sortedPayments
        );
    }

    private BigDecimal surcharge(Payment payment, long daysLate) {
        PaymentGroup group = payment.getPaymentGroup();
        if (group == null) {
            return BigDecimal.ZERO;
        }
        return OverdueCalculator.calculateSurcharge(
                payment.getOriginalValue(), group.getLateFeeRate(), group.getMonthlyInterestRate(), daysLate);
    }
}
