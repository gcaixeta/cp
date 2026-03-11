package dev.gustavorosa.cpsystem.boleto.dto;

import dev.gustavorosa.cpsystem.boleto.model.Boleto;

import java.time.LocalDateTime;

public record BoletoResponse(
    Long id,
    Long paymentId,
    String bankType,
    String bankBoletoId,
    String barcode,
    String digitableLine,
    String pdfUrl,
    String status,
    String errorMessage,
    LocalDateTime createdAt
) {
    public static BoletoResponse fromBoleto(Boleto boleto) {
        return new BoletoResponse(
            boleto.getId(),
            boleto.getPayment().getId(),
            boleto.getBankType().name(),
            boleto.getBankBoletoId(),
            boleto.getBarcode(),
            boleto.getDigitableLine(),
            boleto.getPdfUrl(),
            boleto.getStatus().name(),
            boleto.getErrorMessage(),
            boleto.getCreatedAt()
        );
    }
}
