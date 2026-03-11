package dev.gustavorosa.cpsystem.service;

import dev.gustavorosa.cpsystem.api.request.UpdatePaymentRequest;
import dev.gustavorosa.cpsystem.api.response.PaymentResponse;
import dev.gustavorosa.cpsystem.exception.PaymentNotFoundException;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
import dev.gustavorosa.cpsystem.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void updatePayment_shouldSetStatusToPaid_whenPaymentDateIsProvided() {
        Payment existingPayment = createMockPayment();
        UpdatePaymentRequest request = new UpdatePaymentRequest(
                null, null, LocalDate.now(), "Paid now"
        );

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentResponse response = paymentService.updatePayment(1L, request);

        assertEquals(PaymentStatus.PAID, response.paymentStatus());
        assertEquals("Paid now", response.observation());
    }

    @Test
    void updatePayment_shouldSetStatusToOverdue_whenPaymentDateIsNullAndDueDatePassed() {
        Payment existingPayment = createMockPayment();
        UpdatePaymentRequest request = new UpdatePaymentRequest(
                null, LocalDate.now().minusDays(5), null, "Overdue logic"
        );

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentResponse response = paymentService.updatePayment(1L, request);

        assertEquals(PaymentStatus.OVERDUE, response.paymentStatus());
    }

    @Test
    void updatePayment_shouldSetStatusToPending_whenPaymentDateIsNullAndDueDateIsFuture() {
        Payment existingPayment = createMockPayment();
        UpdatePaymentRequest request = new UpdatePaymentRequest(
                null, LocalDate.now().plusDays(5), null, "Pending logic"
        );

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(existingPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentResponse response = paymentService.updatePayment(1L, request);

        assertEquals(PaymentStatus.PENDING, response.paymentStatus());
    }

    @Test
    void updatePayment_shouldThrowException_whenPaymentNotFound() {
        when(paymentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () ->
                paymentService.updatePayment(1L, new UpdatePaymentRequest(null, null, null, null)));
    }

    // ==========================================
    // CP-31: Recalcular overdueValue ao alterar paymentDate
    // ==========================================

    @Test
    void updatePayment_shouldRecalculateOverdueValue_whenPaymentDateIsAfterDueDate() {
        // Pagamento vencido há 30 dias, pagando hoje
        Payment payment = createMockPaymentWithRates();
        payment.setDueDate(LocalDate.now().minusDays(30));

        UpdatePaymentRequest request = new UpdatePaymentRequest(
                null, null, LocalDate.now(), null
        );

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentResponse response = paymentService.updatePayment(1L, request);

        // original=1000, multa=2% (20), juros=10%/30*30dias=100, total=1120.00
        assertEquals(PaymentStatus.PAID_LATE, response.paymentStatus());
        assertNotNull(response.overdueValue());
        assertEquals(0, new BigDecimal("1120.00").compareTo(response.overdueValue()));
    }

    @Test
    void updatePayment_shouldClearOverdueValue_whenPaymentDateChangedToBeforeDueDate() {
        // Pagamento com vencimento futuro, pagando antes do vencimento
        Payment payment = createMockPaymentWithRates();
        payment.setDueDate(LocalDate.now().plusDays(5));
        payment.setOverdueValue(new BigDecimal("1120.00")); // valor antigo erroneamente definido

        UpdatePaymentRequest request = new UpdatePaymentRequest(
                null, null, LocalDate.now(), null
        );

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentResponse response = paymentService.updatePayment(1L, request);

        assertEquals(PaymentStatus.PAID, response.paymentStatus());
        assertNull(response.overdueValue());
    }

    @Test
    void updatePayment_shouldRecalculateOverdueValue_whenPaymentDateIsChanged() {
        // Pagamento vencido há 60 dias, mudando data de pagamento para 15 dias depois do vencimento
        Payment payment = createMockPaymentWithRates();
        LocalDate dueDate = LocalDate.now().minusDays(60);
        payment.setDueDate(dueDate);
        LocalDate newPaymentDate = dueDate.plusDays(15);

        UpdatePaymentRequest request = new UpdatePaymentRequest(
                null, null, newPaymentDate, null
        );

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentResponse response = paymentService.updatePayment(1L, request);

        // original=1000, multa=2% (20), juros=10%/30*15dias=50, total=1070.00
        assertEquals(PaymentStatus.PAID_LATE, response.paymentStatus());
        assertNotNull(response.overdueValue());
        assertEquals(0, new BigDecimal("1070.00").compareTo(response.overdueValue()));
    }

    @Test
    void updatePayment_shouldRecalculateWithZeroRates_whenGroupHasNoRates() {
        // Pagamento vencido, mas grupo sem taxas configuradas
        Payment payment = createMockPayment();
        payment.setDueDate(LocalDate.now().minusDays(10));

        UpdatePaymentRequest request = new UpdatePaymentRequest(
                null, null, LocalDate.now(), null
        );

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentResponse response = paymentService.updatePayment(1L, request);

        // Sem taxas: overdueValue = originalValue (100) + 0 multa + 0 juros = 100.00
        assertEquals(PaymentStatus.PAID_LATE, response.paymentStatus());
        assertNotNull(response.overdueValue());
        assertEquals(0, new BigDecimal("100.00").compareTo(response.overdueValue()));
    }

    @Test
    void markAsPaid_shouldRecalculateOverdueValue_whenPaymentIsOverdue() {
        Payment payment = createMockPaymentWithRates();
        payment.setDueDate(LocalDate.now().minusDays(30));
        payment.setPaymentStatus(PaymentStatus.OVERDUE);

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentResponse response = paymentService.markAsPaid(1L);

        assertEquals(PaymentStatus.PAID_LATE, response.paymentStatus());
        assertNotNull(response.overdueValue());
        assertTrue(response.overdueValue().compareTo(new BigDecimal("1000")) > 0);
    }

    @Test
    void markAsPaid_shouldNotSetOverdueValue_whenPaymentIsNotOverdue() {
        Payment payment = createMockPaymentWithRates();
        payment.setDueDate(LocalDate.now().plusDays(5));

        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArguments()[0]);

        PaymentResponse response = paymentService.markAsPaid(1L);

        assertEquals(PaymentStatus.PAID, response.paymentStatus());
        assertNull(response.overdueValue());
    }

    private Payment createMockPayment() {
        Client client = new Client();
        client.setId(10L);

        PaymentGroup group = new PaymentGroup();
        group.setId(20L);

        return Payment.builder()
                .id(1L)
                .client(client)
                .paymentGroup(group)
                .payerName("Payer")
                .installmentNumber(1)
                .totalInstallments(1)
                .originalValue(BigDecimal.valueOf(100.0))
                .dueDate(LocalDate.now().plusDays(1))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }

    private Payment createMockPaymentWithRates() {
        Client client = new Client();
        client.setId(10L);

        PaymentGroup group = new PaymentGroup();
        group.setId(20L);
        group.setGroupName("Test Group");
        group.setLateFeeRate(new BigDecimal("0.02"));           // 2% multa
        group.setMonthlyInterestRate(new BigDecimal("0.10"));   // 10% a.m. juros

        return Payment.builder()
                .id(1L)
                .client(client)
                .paymentGroup(group)
                .payerName("Payer")
                .installmentNumber(1)
                .totalInstallments(1)
                .originalValue(new BigDecimal("1000.00"))
                .dueDate(LocalDate.now().plusDays(1))
                .paymentStatus(PaymentStatus.PENDING)
                .build();
    }
}

