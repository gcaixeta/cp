package dev.gustavorosa.cpsystem.scheduled;

import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdatePaymentStatusJobTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private UpdatePaymentStatusJob updatePaymentStatusJob;

    @Test
    void updatePaymentOverdueValues_shouldCalculateCorrectFees() {
        // Given
        PaymentGroup group = PaymentGroup.builder()
                .lateFeeRate(BigDecimal.valueOf(0.02)) // 2% multa
                .monthlyInterestRate(BigDecimal.valueOf(0.03)) // 3% ao mês
                .build();

        Payment payment = Payment.builder()
                .originalValue(BigDecimal.valueOf(100.00))
                .dueDate(LocalDate.now().minusDays(10)) // 10 dias de atraso
                .paymentGroup(group)
                .build();

        when(paymentRepository.findAllOverdueWithGroup()).thenReturn(List.of(payment));

        // When
        updatePaymentStatusJob.updatePaymentOverdueValues();

        // Then
        // Multa: 100 * 0.02 = 2.00
        // Juros: (100 * (0.03 / 30)) * 10 dias = 1.00
        // Acréscimo total: 3.00
        // Valor total com atraso (overdueValue): 100.00 + 3.00 = 103.00
        
        ArgumentCaptor<List<Payment>> captor = ArgumentCaptor.forClass(List.class);
        verify(paymentRepository).saveAll(captor.capture());
        
        Payment updatedPayment = captor.getValue().get(0);
        assertEquals(0, BigDecimal.valueOf(103.00).compareTo(updatedPayment.getOverdueValue()));
        assertEquals(LocalDate.now(), updatedPayment.getOverdueValueDate());
    }

    @Test
    void updatePaymentOverdueValues_shouldCalculateCorrectFees_forClientReportedCase() {
        // Caso reportado pela cliente: original R$ 990,00, multa 2%, juros 10% a.m., 1 dia de atraso
        PaymentGroup group = PaymentGroup.builder()
                .lateFeeRate(BigDecimal.valueOf(0.02)) // 2% multa
                .monthlyInterestRate(BigDecimal.valueOf(0.10)) // 10% ao mês
                .build();

        Payment payment = Payment.builder()
                .originalValue(new BigDecimal("990.00"))
                .dueDate(LocalDate.now().minusDays(1)) // 1 dia de atraso
                .paymentGroup(group)
                .build();

        when(paymentRepository.findAllOverdueWithGroup()).thenReturn(List.of(payment));

        updatePaymentStatusJob.updatePaymentOverdueValues();

        // Multa: 990,00 * 0,02 = 19,80
        // Juros: 990,00 * (0,10 / 30) * 1 dia = 3,30 (exato: 99/30)
        // Total: 990,00 + 19,80 + 3,30 = 1013,10
        ArgumentCaptor<List<Payment>> captor = ArgumentCaptor.forClass(List.class);
        verify(paymentRepository).saveAll(captor.capture());

        Payment updatedPayment = captor.getValue().get(0);
        assertEquals(0, new BigDecimal("1013.10").compareTo(updatedPayment.getOverdueValue()));
    }

    @Test
    void updatePaymentOverdueValues_shouldDoNothing_ifDaysOverdueIsZeroOrLess() {
        Payment payment = Payment.builder()
                .dueDate(LocalDate.now())
                .paymentGroup(new PaymentGroup())
                .build();

        when(paymentRepository.findAllOverdueWithGroup()).thenReturn(List.of(payment));

        updatePaymentStatusJob.updatePaymentOverdueValues();

        verify(paymentRepository).saveAll(anyList());
        assertNull(payment.getOverdueValue());
    }
}

