package dev.gustavorosa.cpsystem.boleto.exception;

public class BankNotSupportedException extends RuntimeException {
    public BankNotSupportedException(String message) {
        super(message);
    }
}
