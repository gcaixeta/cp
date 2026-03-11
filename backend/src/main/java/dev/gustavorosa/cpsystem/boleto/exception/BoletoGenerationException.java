package dev.gustavorosa.cpsystem.boleto.exception;

public class BoletoGenerationException extends RuntimeException {
    public BoletoGenerationException(String message) {
        super(message);
    }
    
    public BoletoGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
