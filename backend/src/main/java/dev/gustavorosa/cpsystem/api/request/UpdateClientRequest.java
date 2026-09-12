package dev.gustavorosa.cpsystem.api.request;

import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateClientRequest(
    @Size(min = 1, max = 100, message = "client name must have between 1 and 100 characters.")
    String clientName,
    String address,
    String phone,
    String document,
    String bank,
    BigDecimal lateFeeRate,
    BigDecimal monthlyInterestRate
) {
}

