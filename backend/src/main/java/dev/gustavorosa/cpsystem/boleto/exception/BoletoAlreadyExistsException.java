package dev.gustavorosa.cpsystem.boleto.exception;

public class BoletoAlreadyExistsException extends RuntimeException {
    public BoletoAlreadyExistsException(String message) {
        super(message);
    }
}
