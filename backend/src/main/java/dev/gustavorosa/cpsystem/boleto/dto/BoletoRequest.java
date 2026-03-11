package dev.gustavorosa.cpsystem.boleto.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@Data
public class BoletoRequest {
    private Long paymentId;
    private BigDecimal amount;
    private LocalDate dueDate;
    private String payerName;
    private String payerDocument;
    private String payerPhone;
    private String description;
    private BigDecimal lateFeeRate;
    private BigDecimal monthlyInterestRate;
}
