package dev.gustavorosa.cpsystem.service;

import dev.gustavorosa.cpsystem.api.response.PaymentGroupResponse;
import dev.gustavorosa.cpsystem.boleto.service.BoletoService;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
import dev.gustavorosa.cpsystem.model.factory.PaymentGroupFactory;
import dev.gustavorosa.cpsystem.repository.PaymentGroupRepository;
import dev.gustavorosa.cpsystem.repository.PaymentRepository;
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
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class PaymentGroupServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGroupRepository paymentGroupRepository;

    @Mock
    private PaymentGroupFactory paymentGroupFactory;

    @Mock
    private BoletoService boletoService;

    @InjectMocks
    private PaymentGroupService paymentGroupService;

    @Test
    void findAllPaymentGroups_shouldReturnAllGroups_whenNoClientFilter() {
        PaymentGroup group = createMockGroup(1L, "Grupo A");
        List<Payment> payments = createMockPayments(group);

        when(paymentGroupRepository.findAll()).thenReturn(List.of(group));
        when(paymentRepository.findByPaymentGroupId(1L)).thenReturn(payments);

        List<PaymentGroupResponse> result = paymentGroupService.findAllPaymentGroups(null);

        assertEquals(1, result.size());
        PaymentGroupResponse response = result.get(0);
        assertEquals("Grupo A", response.groupName());
        assertEquals("Test Client", response.clientName());
        assertEquals(3, response.totalInstallments());
    }

    @Test
    void findAllPaymentGroups_shouldFilterByClient_whenClientIdProvided() {
        PaymentGroup group = createMockGroup(1L, "Grupo B");
        List<Payment> payments = createMockPayments(group);

        when(paymentGroupRepository.findByClientId(10L)).thenReturn(List.of(group));
        when(paymentRepository.findByPaymentGroupId(1L)).thenReturn(payments);

        List<PaymentGroupResponse> result = paymentGroupService.findAllPaymentGroups(10L);

        assertEquals(1, result.size());
        assertEquals("Grupo B", result.get(0).groupName());
    }

    @Test
    void findAllPaymentGroups_shouldReturnEmptyList_whenNoGroupsExist() {
        when(paymentGroupRepository.findAll()).thenReturn(List.of());

        List<PaymentGroupResponse> result = paymentGroupService.findAllPaymentGroups(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllPaymentGroups_shouldCalculatePaidInstallmentsCorrectly() {
        PaymentGroup group = createMockGroup(1L, "Grupo C");
        List<Payment> payments = createMockPayments(group); // 1 PAID, 1 OVERDUE, 1 PENDING

        when(paymentGroupRepository.findAll()).thenReturn(List.of(group));
        when(paymentRepository.findByPaymentGroupId(1L)).thenReturn(payments);

        PaymentGroupResponse response = paymentGroupService.findAllPaymentGroups(null).get(0);

        assertEquals(1, response.paidInstallments());
        assertEquals(3, response.totalInstallments());
    }

    @Test
    void findAllPaymentGroups_shouldCalculateTotalPaidCorrectly() {
        PaymentGroup group = createMockGroup(1L, "Grupo D");
        List<Payment> payments = createMockPayments(group);

        when(paymentGroupRepository.findAll()).thenReturn(List.of(group));
        when(paymentRepository.findByPaymentGroupId(1L)).thenReturn(payments);

        PaymentGroupResponse response = paymentGroupService.findAllPaymentGroups(null).get(0);

        // Only PAID payment: originalValue = 500
        assertEquals(0, new BigDecimal("500.00").compareTo(response.totalPaid()));
    }

    @Test
    void findAllPaymentGroups_shouldCalculateTotalRemainingCorrectly() {
        PaymentGroup group = createMockGroup(1L, "Grupo E");
        List<Payment> payments = createMockPayments(group);

        when(paymentGroupRepository.findAll()).thenReturn(List.of(group));
        when(paymentRepository.findByPaymentGroupId(1L)).thenReturn(payments);

        PaymentGroupResponse response = paymentGroupService.findAllPaymentGroups(null).get(0);

        // OVERDUE (overdueValue=600) + PENDING (originalValue=500) = 1100
        assertEquals(0, new BigDecimal("1100.00").compareTo(response.totalRemaining()));
    }

    @Test
    void findAllPaymentGroups_shouldUsePaidLateAsPartOfPaid() {
        PaymentGroup group = createMockGroup(1L, "Grupo F");
        Client client = group.getClient();

        Payment paidLate = Payment.builder()
                .id(1L).client(client).paymentGroup(group).payerName("Payer")
                .installmentNumber(1).totalInstallments(2)
                .originalValue(new BigDecimal("500.00"))
                .overdueValue(new BigDecimal("550.00"))
                .dueDate(LocalDate.now().minusDays(10))
                .paymentDate(LocalDate.now())
                .paymentStatus(PaymentStatus.PAID_LATE)
                .build();

        Payment pending = Payment.builder()
                .id(2L).client(client).paymentGroup(group).payerName("Payer")
                .installmentNumber(2).totalInstallments(2)
                .originalValue(new BigDecimal("500.00"))
                .dueDate(LocalDate.now().plusDays(20))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        when(paymentGroupRepository.findAll()).thenReturn(List.of(group));
        when(paymentRepository.findByPaymentGroupId(1L)).thenReturn(List.of(paidLate, pending));

        PaymentGroupResponse response = paymentGroupService.findAllPaymentGroups(null).get(0);

        assertEquals(1, response.paidInstallments());
        // PAID_LATE uses overdueValue (550)
        assertEquals(0, new BigDecimal("550.00").compareTo(response.totalPaid()));
        assertEquals(0, new BigDecimal("500.00").compareTo(response.totalRemaining()));
    }

    // ==========================================
    // CP-29: Excluir grupo de pagamento
    // ==========================================

    @Test
    void deletePaymentGroup_shouldDeletePaymentsAndGroup() {
        PaymentGroup group = createMockGroup(1L, "Grupo Delete");
        List<Payment> payments = createMockPayments(group);

        when(paymentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(paymentRepository.findByPaymentGroupId(1L)).thenReturn(payments);

        paymentGroupService.deletePaymentGroup(1L);

        verify(paymentRepository).deleteAll(payments);
        verify(paymentGroupRepository).delete(group);
    }

    @Test
    void deletePaymentGroup_shouldDeleteGroupWithNoPayments() {
        PaymentGroup group = createMockGroup(1L, "Grupo Empty");

        when(paymentGroupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(paymentRepository.findByPaymentGroupId(1L)).thenReturn(List.of());

        paymentGroupService.deletePaymentGroup(1L);

        verify(paymentRepository).deleteAll(List.of());
        verify(paymentGroupRepository).delete(group);
    }

    @Test
    void deletePaymentGroup_shouldThrowException_whenGroupNotFound() {
        when(paymentGroupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                paymentGroupService.deletePaymentGroup(99L));

        verify(paymentRepository, never()).deleteAll(any());
        verify(paymentGroupRepository, never()).delete(any());
    }

    private PaymentGroup createMockGroup(Long id, String name) {
        Client client = new Client();
        client.setId(10L);
        client.setName("Test Client");

        PaymentGroup group = new PaymentGroup();
        group.setId(id);
        group.setGroupName(name);
        group.setClient(client);
        group.setPayerDocument("12345678901");
        group.setPayerPhone("11999999999");
        group.setTotalInstallments(3);
        group.setLateFeeRate(new BigDecimal("0.02"));
        group.setMonthlyInterestRate(new BigDecimal("0.10"));
        group.setCreationDate(LocalDate.now());
        return group;
    }

    private List<Payment> createMockPayments(PaymentGroup group) {
        Client client = group.getClient();

        Payment paid = Payment.builder()
                .id(1L).client(client).paymentGroup(group).payerName("Payer")
                .installmentNumber(1).totalInstallments(3)
                .originalValue(new BigDecimal("500.00"))
                .dueDate(LocalDate.now().minusDays(30))
                .paymentDate(LocalDate.now().minusDays(30))
                .paymentStatus(PaymentStatus.PAID)
                .build();

        Payment overdue = Payment.builder()
                .id(2L).client(client).paymentGroup(group).payerName("Payer")
                .installmentNumber(2).totalInstallments(3)
                .originalValue(new BigDecimal("500.00"))
                .overdueValue(new BigDecimal("600.00"))
                .dueDate(LocalDate.now().minusDays(5))
                .paymentStatus(PaymentStatus.OVERDUE)
                .build();

        Payment pending = Payment.builder()
                .id(3L).client(client).paymentGroup(group).payerName("Payer")
                .installmentNumber(3).totalInstallments(3)
                .originalValue(new BigDecimal("500.00"))
                .dueDate(LocalDate.now().plusDays(25))
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        return List.of(paid, overdue, pending);
    }
}
