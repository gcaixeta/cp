package dev.gustavorosa.cpsystem.boleto.service;

import dev.gustavorosa.cpsystem.boleto.dto.BankApiResponse;
import dev.gustavorosa.cpsystem.boleto.dto.BoletoRequest;
import dev.gustavorosa.cpsystem.boleto.dto.BoletoResponse;
import dev.gustavorosa.cpsystem.boleto.exception.BoletoAlreadyExistsException;
import dev.gustavorosa.cpsystem.boleto.exception.BoletoGenerationException;
import dev.gustavorosa.cpsystem.boleto.model.Boleto;
import dev.gustavorosa.cpsystem.boleto.model.BoletoStatus;
import dev.gustavorosa.cpsystem.boleto.model.BankType;
import dev.gustavorosa.cpsystem.boleto.repository.BoletoRepository;
import dev.gustavorosa.cpsystem.boleto.service.strategy.BankStrategyFactory;
import dev.gustavorosa.cpsystem.boleto.service.strategy.BankBoletoStrategy;
import dev.gustavorosa.cpsystem.exception.PaymentNotFoundException;
import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class BoletoService {
    
    private final BoletoRepository boletoRepository;
    private final PaymentRepository paymentRepository;
    private final BankStrategyFactory bankStrategyFactory;
    
    public BoletoService(
        BoletoRepository boletoRepository,
        PaymentRepository paymentRepository,
        BankStrategyFactory bankStrategyFactory
    ) {
        this.boletoRepository = boletoRepository;
        this.paymentRepository = paymentRepository;
        this.bankStrategyFactory = bankStrategyFactory;
    }
    
    @Transactional
    public BoletoResponse generateBoletoForPayment(Long paymentId, BankType bankType) {
        log.info("Iniciando geração de boleto para payment {} no banco {}", paymentId, bankType);
        
        // 1. Buscar pagamento
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Pagamento não encontrado: " + paymentId));
        
        // 2. Verificar se já existe boleto
        if (boletoRepository.existsByPayment(payment)) {
            throw new BoletoAlreadyExistsException("Boleto já existe para o pagamento " + paymentId);
        }
        
        // 3. Validar pagamento
        validatePayment(payment);
        
        // 4. Criar request para o banco
        BoletoRequest request = BoletoRequest.builder()
            .paymentId(payment.getId())
            .amount(payment.getOriginalValue())
            .dueDate(payment.getDueDate())
            .payerName(payment.getPayerName())
            .payerDocument(payment.getPayerDocument())
            .payerPhone(payment.getPaymentGroup().getPayerPhone())
            .description(payment.getPaymentGroup().getGroupName())
            .lateFeeRate(payment.getClient().getLateFeeRate())
            .monthlyInterestRate(payment.getClient().getMonthlyInterestRate())
            .build();
        
        // 5. Selecionar estratégia do banco
        BankBoletoStrategy strategy = bankStrategyFactory.getStrategy(bankType);
        
        // 6. Gerar boleto na API do banco
        BankApiResponse bankResponse = strategy.generateBoleto(request);
        
        // 7. Salvar registro no banco de dados
        Boleto boleto = Boleto.builder()
            .payment(payment)
            .bankType(bankType)
            .bankBoletoId(bankResponse.getBankBoletoId())
            .barcode(bankResponse.getBarcode())
            .digitableLine(bankResponse.getDigitableLine())
            .pdfUrl(bankResponse.getPdfUrl())
            .status(bankResponse.isSuccess() ? BoletoStatus.GENERATED : BoletoStatus.ERROR)
            .bankApiResponse(bankResponse.getRawResponse())
            .errorMessage(bankResponse.getErrorMessage())
            .build();
        
        Boleto savedBoleto = boletoRepository.save(boleto);
        
        log.info("Boleto {} para payment {} com status {}", 
            bankResponse.isSuccess() ? "gerado com sucesso" : "com erro",
            paymentId,
            savedBoleto.getStatus());
        
        // 8. Retornar resposta
        return BoletoResponse.fromBoleto(savedBoleto);
    }
    
    @Transactional(readOnly = true)
    public BoletoResponse getBoletoByPaymentId(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Pagamento não encontrado: " + paymentId));
        
        Boleto boleto = boletoRepository.findByPayment(payment)
            .orElseThrow(() -> new BoletoGenerationException("Boleto não encontrado para o pagamento " + paymentId));
        
        return BoletoResponse.fromBoleto(boleto);
    }
    
    @Transactional
    public BoletoResponse retryBoletoGeneration(Long paymentId, BankType bankType) {
        log.info("Tentando reprocessar boleto para payment {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Pagamento não encontrado: " + paymentId));
        
        // Buscar boleto existente
        Boleto existingBoleto = boletoRepository.findByPayment(payment)
            .orElse(null);
        
        // Se existe e não está com erro, não permite reprocessar
        if (existingBoleto != null && existingBoleto.getStatus() != BoletoStatus.ERROR) {
            throw new BoletoGenerationException("Boleto já foi gerado com sucesso para o pagamento " + paymentId);
        }
        
        // Se existe com erro, deletar para tentar novamente
        if (existingBoleto != null) {
            boletoRepository.delete(existingBoleto);
        }
        
        // Tentar gerar novamente
        return generateBoletoForPayment(paymentId, bankType);
    }
    
    private void validatePayment(Payment payment) {
        if (payment.getOriginalValue() == null || payment.getOriginalValue().signum() <= 0) {
            throw new BoletoGenerationException("Valor do pagamento inválido");
        }
        
        if (payment.getDueDate() == null) {
            throw new BoletoGenerationException("Data de vencimento não informada");
        }
        
        if (payment.getPayerName() == null || payment.getPayerName().isEmpty()) {
            throw new BoletoGenerationException("Nome do pagador não informado");
        }
        
        if (payment.getPayerDocument() == null || payment.getPayerDocument().isEmpty()) {
            throw new BoletoGenerationException("Documento do pagador não informado");
        }
    }
}
