package dev.gustavorosa.cpsystem.service;

import dev.gustavorosa.cpsystem.api.request.UpdatePaymentRequest;
import dev.gustavorosa.cpsystem.api.response.GroupedPaymentResponse;
import dev.gustavorosa.cpsystem.api.response.PaymentResponse;
import dev.gustavorosa.cpsystem.exception.PaymentNotFoundException;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
import dev.gustavorosa.cpsystem.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    public List<PaymentResponse> findAllPayment() {
        return paymentRepository.findAll().stream().map(Payment::toResponse).toList();
    }

    public List<GroupedPaymentResponse> findGroupedPayments(Long clientId, PaymentStatus status, Integer month, Integer year) {
        LocalDate startDate;
        LocalDate endDate;

        if (month != null && year != null) {
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        } else {
            startDate = LocalDate.now().withDayOfMonth(1);
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        }

        String statusString = status != null ? status.name() : null;
        List<Payment> mainPayments = paymentRepository.findFilteredPayments(clientId, statusString, startDate, endDate);
        List<Payment> allOverdue = paymentRepository.findOverduePayments(clientId);

        Map<Long, List<Payment>> overdueByGroup = allOverdue.stream()
                .filter(p -> p.getPaymentGroup() != null)
                .collect(Collectors.groupingBy(p -> p.getPaymentGroup().getId()));

        Set<Long> processedPaymentIds = new HashSet<>();
        List<GroupedPaymentResponse> result = new ArrayList<>();

        // Process main payments
        for (Payment p : mainPayments) {
            Long groupId = p.getPaymentGroup() != null ? p.getPaymentGroup().getId() : null;
            List<PaymentResponse> overdueNested = new ArrayList<>();

            if (groupId != null && overdueByGroup.containsKey(groupId)) {
                overdueNested = overdueByGroup.get(groupId).stream()
                        .filter(ov -> !ov.getId().equals(p.getId()))
                        .sorted(Comparator.comparing(Payment::getDueDate))
                        .map(Payment::toResponse)
                        .collect(Collectors.toList());

                overdueByGroup.get(groupId).forEach(ov -> processedPaymentIds.add(ov.getId()));
            }

            result.add(new GroupedPaymentResponse(p.toResponse(), overdueNested));
            processedPaymentIds.add(p.getId());
        }

        // Process remaining overdue payments that weren't in mainPayments
        for (Map.Entry<Long, List<Payment>> entry : overdueByGroup.entrySet()) {
            List<Payment> overdueList = entry.getValue().stream()
                    .filter(p -> !processedPaymentIds.contains(p.getId()))
                    .sorted(Comparator.comparing(Payment::getDueDate))
                    .collect(Collectors.toList());

            if (!overdueList.isEmpty()) {
                Payment oldest = overdueList.get(0);
                List<PaymentResponse> rest = overdueList.stream()
                        .skip(1)
                        .map(Payment::toResponse)
                        .collect(Collectors.toList());

                result.add(new GroupedPaymentResponse(oldest.toResponse(), rest));
                overdueList.forEach(p -> processedPaymentIds.add(p.getId()));
            }
        }

        return result;
    }

    public PaymentResponse updatePayment(Long id, UpdatePaymentRequest request) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment with id " + id + " not found"));

        if (request.originalValue() != null) {
            payment.setOriginalValue(request.originalValue());
        }
        if (request.dueDate() != null) {
            payment.setDueDate(request.dueDate());
        }
        if (request.paymentDate() != null) {
            payment.setPaymentDate(request.paymentDate());
        }
        if (request.observation() != null) {
            payment.setObservation(request.observation());
        }

        updateStatus(payment);
        recalculateOverdueValue(payment);

        return paymentRepository.save(payment).toResponse();
    }

    public PaymentResponse markAsPaid(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment with id " + id + " not found"));

        payment.setPaymentDate(LocalDate.now());

        // Check if payment is late
        if (payment.getDueDate().isBefore(LocalDate.now())) {
            payment.setPaymentStatus(PaymentStatus.PAID_LATE);
        } else {
            payment.setPaymentStatus(PaymentStatus.PAID);
        }

        recalculateOverdueValue(payment);

        return paymentRepository.save(payment).toResponse();
    }

    private void recalculateOverdueValue(Payment payment) {
        PaymentGroup group = payment.getPaymentGroup();
        if (group == null) {
            return;
        }

        LocalDate referenceDate = payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDate.now();
        long daysOverdue = referenceDate.toEpochDay() - payment.getDueDate().toEpochDay();

        if (daysOverdue <= 0) {
            payment.setOverdueValue(null);
            payment.setOverdueValueDate(null);
            return;
        }

        BigDecimal originalValue = payment.getOriginalValue();
        BigDecimal lateFeeRate = group.getLateFeeRate() != null ? group.getLateFeeRate() : BigDecimal.ZERO;
        BigDecimal monthlyInterestRate = group.getMonthlyInterestRate() != null ? group.getMonthlyInterestRate() : BigDecimal.ZERO;

        BigDecimal lateFee = originalValue.multiply(lateFeeRate);
        BigDecimal dailyInterestRate = monthlyInterestRate.divide(BigDecimal.valueOf(30), 10, RoundingMode.HALF_UP);
        BigDecimal totalInterest = originalValue.multiply(dailyInterestRate).multiply(BigDecimal.valueOf(daysOverdue));
        BigDecimal newOverdueValue = originalValue.add(lateFee).add(totalInterest).setScale(2, RoundingMode.HALF_UP);

        payment.setOverdueValue(newOverdueValue);
        payment.setOverdueValueDate(referenceDate);
    }

    private void updateStatus(Payment payment) {
        if (payment.getPaymentDate() != null) {
            // Check if payment was made after due date
            if (payment.getDueDate().isBefore(payment.getPaymentDate())) {
                payment.setPaymentStatus(PaymentStatus.PAID_LATE);
            } else {
            payment.setPaymentStatus(PaymentStatus.PAID);
            }
        } else {
            if (payment.getDueDate().isBefore(LocalDate.now())) {
                payment.setPaymentStatus(PaymentStatus.OVERDUE);
            } else {
                payment.setPaymentStatus(PaymentStatus.PENDING);
            }
        }
    }
}
