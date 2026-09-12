package dev.gustavorosa.cpsystem.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePaymentGroupRequest(
    @NotNull(message = "client id must not be null.") Long clientId,
    @NotBlank(message = "payer name must not be blank.")
    @Size(max = 50, message = "payer name must have at most 50 characters.")
    String payerName,
    @NotNull(message = "payer document must not be null.") String payerDocument,
    String payerPhone,
    @NotNull(message = "monthly value must not be null.") BigDecimal monthlyValue,
    @NotNull(message = "total installments must not be null.") Integer totalInstallments,
    BigDecimal lateFeeRate,
    BigDecimal monthlyInterestRate,
    @NotNull(message = "first installment due date must not be null") LocalDate firstInstallmentDueDate,
    String observation,
    Boolean generateBoletos
) {

}
