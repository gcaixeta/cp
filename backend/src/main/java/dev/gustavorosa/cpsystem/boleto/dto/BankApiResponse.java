package dev.gustavorosa.cpsystem.boleto.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BankApiResponse {
    private boolean success;
    private String bankBoletoId;
    private String barcode;
    private String digitableLine;
    private String pdfUrl;
    private String rawResponse;
    private String errorMessage;
}
