package dev.gustavorosa.cpsystem.boleto.service.strategy;

import dev.gustavorosa.cpsystem.boleto.dto.BankApiResponse;
import dev.gustavorosa.cpsystem.boleto.dto.BoletoRequest;
import dev.gustavorosa.cpsystem.boleto.model.BankType;

public interface BankBoletoStrategy {
    BankApiResponse generateBoleto(BoletoRequest request);
    BankType getSupportedBank();
}
