package dev.gustavorosa.cpsystem.boleto.controller;

import dev.gustavorosa.cpsystem.boleto.dto.BoletoResponse;
import dev.gustavorosa.cpsystem.boleto.model.BankType;
import dev.gustavorosa.cpsystem.boleto.service.BoletoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/boletos")
@Slf4j
public class BoletoController {
    
    private final BoletoService boletoService;
    
    public BoletoController(BoletoService boletoService) {
        this.boletoService = boletoService;
    }
    
    @GetMapping("/payment/{paymentId}")
    public ResponseEntity<BoletoResponse> getBoletoByPaymentId(@PathVariable Long paymentId) {
        log.info("[Entry - BoletoController.getBoletoByPaymentId] - Getting boleto for payment: {}", paymentId);
        BoletoResponse response = boletoService.getBoletoByPaymentId(paymentId);
        log.info("[Exit - BoletoController.getBoletoByPaymentId] - Boleto retrieved successfully");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/payment/{paymentId}/retry")
    public ResponseEntity<BoletoResponse> retryBoletoGeneration(
        @PathVariable Long paymentId,
        @RequestParam(defaultValue = "INTER") BankType bankType
    ) {
        log.info("[Entry - BoletoController.retryBoletoGeneration] - Retrying boleto generation for payment: {}", paymentId);
        BoletoResponse response = boletoService.retryBoletoGeneration(paymentId, bankType);
        log.info("[Exit - BoletoController.retryBoletoGeneration] - Boleto retry completed");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/payment/{paymentId}/generate")
    public ResponseEntity<BoletoResponse> generateBoleto(
        @PathVariable Long paymentId,
        @RequestParam(defaultValue = "INTER") BankType bankType
    ) {
        log.info("[Entry - BoletoController.generateBoleto] - Generating boleto for payment: {}", paymentId);
        BoletoResponse response = boletoService.generateBoletoForPayment(paymentId, bankType);
        log.info("[Exit - BoletoController.generateBoleto] - Boleto generated successfully");
        return ResponseEntity.ok(response);
    }
}
