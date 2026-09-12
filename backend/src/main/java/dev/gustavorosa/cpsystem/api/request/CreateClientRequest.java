package dev.gustavorosa.cpsystem.api.request;

import dev.gustavorosa.cpsystem.model.Client;
import dev.gustavorosa.cpsystem.utils.NameUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateClientRequest(
    @NotBlank(message = "client name must not be blank.")
    @Size(max = 100, message = "client name must have at most 100 characters.")
    String clientName,
    String address,
    String phone,
    String document,
    String bank,
    BigDecimal lateFeeRate,
    BigDecimal monthlyInterestRate
) {

    public Client toModel() {
        return Client.builder()
                .name(NameUtils.toTitleCase(clientName))
                .address(address)
                .bank(bank)
                .document(document)
                .phone(phone)
                .lateFeeRate(lateFeeRate)
                .monthlyInterestRate(monthlyInterestRate)
                .build();
    }
}
