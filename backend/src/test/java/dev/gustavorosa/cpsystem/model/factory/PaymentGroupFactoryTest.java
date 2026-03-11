package dev.gustavorosa.cpsystem.model.factory;

import dev.gustavorosa.cpsystem.api.request.CreatePaymentGroupRequest;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
import dev.gustavorosa.cpsystem.repository.ClientRepository;
import dev.gustavorosa.cpsystem.repository.PaymentGroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGroupFactoryTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PaymentGroupRepository paymentGroupRepository;

    @InjectMocks
    private PaymentGroupFactory paymentGroupFactory;

    @Test
    void buildPaymentList_shouldCreateCorrectNumberOfInstallments() {
        PaymentGroup group = PaymentGroup.builder()
                .client(new Client())
                .totalInstallments(3)
                .payerDocument("123456789")
                .build();

        CreatePaymentGroupRequest request = new CreatePaymentGroupRequest(
                1L, "Payer Name", "123456789", "11999999999", BigDecimal.valueOf(100.0), 3,
                null, null, LocalDate.now().plusDays(1), "Obs", false
        );

        List<Payment> payments = paymentGroupFactory.buildPaymentList(group, request);

        assertEquals(3, payments.size());
        assertEquals(1, payments.get(0).getInstallmentNumber());
        assertEquals(2, payments.get(1).getInstallmentNumber());
        assertEquals(3, payments.get(2).getInstallmentNumber());
    }

    @Test
    void buildPaymentList_shouldSetCorrectStatusBasedOnDueDate() {
        PaymentGroup group = PaymentGroup.builder()
                .client(new Client())
                .totalInstallments(2)
                .build();

        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        CreatePaymentGroupRequest request = new CreatePaymentGroupRequest(
                1L, "Payer Name", "123456789", "11999999999", BigDecimal.valueOf(100.0), 2,
                null, null, yesterday, "Obs", false
        );

        // First installment is yesterday (OVERDUE), second is tomorrow (PENDING)
        // Note: buildPaymentList uses .plusMonths((i-1))
        // So for i=1, dueDate = yesterday
        // For i=2, dueDate = yesterday + 1 month (PENDING)

        List<Payment> payments = paymentGroupFactory.buildPaymentList(group, request);

        assertEquals(PaymentStatus.OVERDUE, payments.get(0).getPaymentStatus());
        assertEquals(PaymentStatus.PENDING, payments.get(1).getPaymentStatus());
    }
}

