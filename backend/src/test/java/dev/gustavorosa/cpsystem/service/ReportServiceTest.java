package dev.gustavorosa.cpsystem.service;

import dev.gustavorosa.cpsystem.api.response.MonthlyReportData;
import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ClientService clientService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PdfReportGenerator pdfReportGenerator;

    @InjectMocks
    private ReportService reportService;

    @Test
    void generateMonthlyReport_shouldSumSurchargeWithFullPrecision_beforeRoundingTheTotal() {
        // Duas parcelas pagas com atraso no mesmo mês: cada uma, individualmente, arredonda o
        // valor com multa/juros para R$ 102,33 (o excedente de 0,333... é descartado por parcela).
        // Somando os valores já arredondados: 102,33 + 102,33 = 204,66.
        // Mas a soma "verdadeira" (sem arredondar cada parcela antes) é 204,666... que arredonda
        // para 204,67 — é esse valor que o total do relatório deve refletir, para não divergir do
        // valor combinado calculado pelo banco quando várias parcelas são somadas.
        PaymentGroup group = PaymentGroup.builder()
                .lateFeeRate(BigDecimal.ZERO)
                .monthlyInterestRate(new BigDecimal("0.10")) // 10% ao mês
                .build();

        LocalDate dueDate = LocalDate.of(2026, 3, 10);
        LocalDate paymentDate = dueDate.plusDays(7);

        Payment payment1 = Payment.builder()
                .originalValue(new BigDecimal("100.00"))
                .dueDate(dueDate)
                .paymentDate(paymentDate)
                .paymentStatus(PaymentStatus.PAID_LATE)
                .paymentGroup(group)
                .build();

        Payment payment2 = Payment.builder()
                .originalValue(new BigDecimal("100.00"))
                .dueDate(dueDate)
                .paymentDate(paymentDate)
                .paymentStatus(PaymentStatus.PAID_LATE)
                .paymentGroup(group)
                .build();

        Client client = new Client();
        client.setId(1L);

        when(clientService.findClientById(1L)).thenReturn(client);
        when(paymentRepository.findByClientIdAndDueDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(paymentRepository.findByClientIdAndPaymentDateBetween(eq(1L), any(), any()))
                .thenReturn(List.of(payment1, payment2));

        reportService.generateMonthlyReport(1L, 3, 2026);

        ArgumentCaptor<MonthlyReportData> captor = ArgumentCaptor.forClass(MonthlyReportData.class);
        verify(pdfReportGenerator).generate(captor.capture());

        assertEquals(0, new BigDecimal("204.67").compareTo(captor.getValue().totalReceived()));
    }
}
