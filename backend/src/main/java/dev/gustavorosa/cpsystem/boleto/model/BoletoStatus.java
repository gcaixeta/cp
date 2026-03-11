package dev.gustavorosa.cpsystem.boleto.model;

public enum BoletoStatus {
    GENERATED,  // Gerado com sucesso
    ERROR,      // Erro na geração
    PAID,       // Pago
    CANCELLED   // Cancelado
}
