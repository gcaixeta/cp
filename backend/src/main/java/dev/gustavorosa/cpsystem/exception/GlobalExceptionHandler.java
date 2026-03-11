package dev.gustavorosa.cpsystem.exception;

import dev.gustavorosa.cpsystem.boleto.exception.BankNotSupportedException;
import dev.gustavorosa.cpsystem.boleto.exception.BoletoAlreadyExistsException;
import dev.gustavorosa.cpsystem.boleto.exception.BoletoGenerationException;
import dev.gustavorosa.cpsystem.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoClientFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoClientFoundException(NoClientFoundException ex) {
        log.error("[ExceptionHandler] - Client not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        log.error("[ExceptionHandler] - Payment not found: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BoletoGenerationException.class)
    public ResponseEntity<ErrorResponse> handleBoletoGenerationException(BoletoGenerationException ex) {
        log.error("[ExceptionHandler] - Boleto generation error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(BoletoAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleBoletoAlreadyExistsException(BoletoAlreadyExistsException ex) {
        log.error("[ExceptionHandler] - Boleto already exists: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.CONFLICT.value(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(BankNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleBankNotSupportedException(BankNotSupportedException ex) {
        log.error("[ExceptionHandler] - Bank not supported: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("[ExceptionHandler] - Internal server error: ", ex);
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

