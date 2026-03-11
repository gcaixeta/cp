package dev.gustavorosa.cpsystem.boleto.service.strategy;

import dev.gustavorosa.cpsystem.boleto.dto.BankApiResponse;
import dev.gustavorosa.cpsystem.boleto.dto.BoletoRequest;
import dev.gustavorosa.cpsystem.boleto.model.BankType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Estratégia Mock para testes de geração de boletos sem integração real
 * Ativa apenas quando bank.mock.enabled=true
 */
@Slf4j
@Component
@Order(1) // Prioridade alta para sobrescrever InterBoletoStrategy quando mock está ativo
@ConditionalOnProperty(name = "bank.mock.enabled", havingValue = "true")
public class MockBoletoStrategy implements BankBoletoStrategy {
    
    @Override
    public BankApiResponse generateBoleto(BoletoRequest request) {
        log.warn("🧪 =====================================");
        log.warn("🧪 MODO TESTE ATIVO - BOLETO MOCK");
        log.warn("🧪 Payment ID: {}", request.getPaymentId());
        log.warn("🧪 Valor: R$ {}", request.getAmount());
        log.warn("🧪 Vencimento: {}", request.getDueDate());
        log.warn("🧪 =====================================");
        
        // Simula um delay de rede realista (300-800ms)
        simulateNetworkDelay();
        
        // Gera dados fictícios mas com formato válido
        String nossoNumero = generateMockNossoNumero(request);
        String barcode = generateMockBarcode(request);
        String digitableLine = generateMockDigitableLine(barcode);
        String pdfUrl = generateMockPdfUrl(nossoNumero);
        String jsonResponse = buildMockJsonResponse(nossoNumero, barcode, digitableLine, pdfUrl, request);
        
        log.info("✅ Boleto MOCK gerado com sucesso");
        log.info("   📄 Nosso Número: {}", nossoNumero);
        log.info("   📊 Código de Barras: {}", barcode);
        log.info("   💳 Linha Digitável: {}", digitableLine);
        log.info("   🔗 PDF URL: {}", pdfUrl);
        
        return BankApiResponse.builder()
            .success(true)
            .bankBoletoId(nossoNumero)
            .barcode(barcode)
            .digitableLine(digitableLine)
            .pdfUrl(pdfUrl)
            .rawResponse(jsonResponse)
            .build();
    }
    
    @Override
    public BankType getSupportedBank() {
        // Sobrescreve o INTER quando mock está ativado
        return BankType.INTER;
    }
    
    private void simulateNetworkDelay() {
        try {
            long delay = 300 + (long) (Math.random() * 500); // 300-800ms
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Simulação de delay interrompida");
        }
    }
    
    private String generateMockNossoNumero(BoletoRequest request) {
        // Formato: MOCK-{timestamp}-{paymentId}-{random}
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(5);
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return String.format("MOCK-%s-%d-%s", timestamp, request.getPaymentId(), randomPart);
    }
    
    private String generateMockBarcode(BoletoRequest request) {
        // Formato padrão de código de barras (44 dígitos)
        // Posições: Banco(3) + Moeda(1) + DV(1) + Fator(4) + Valor(10) + Campo Livre(25)
        
        String banco = "077"; // Banco Inter
        String moeda = "9";   // Real
        String fatorVencimento = calculateFatorVencimento(request.getDueDate());
        String valor = formatValor(request.getAmount().doubleValue());
        
        // Campo livre simulado (25 dígitos)
        String campoLivre = String.format("%025d", request.getPaymentId() * 1000000L + System.currentTimeMillis() % 1000000L);
        
        // Calcula DV (simplificado - apenas para mock)
        String semDV = banco + moeda + fatorVencimento + valor + campoLivre;
        String dv = calculateMockDV(semDV);
        
        // Monta o código de barras completo
        return banco + moeda + dv + fatorVencimento + valor + campoLivre;
    }
    
    private String generateMockDigitableLine(String barcode) {
        // Converte código de barras em linha digitável
        // Formato padrão: AAAAA.AAAAX BBBBB.BBBBBX CCCCC.CCCCCX D EEEEEEEEEEEEEE
        // Onde:
        // - AAAA AAAAA = Banco (3) + Moeda (1) + Primeiros 5 dígitos do campo livre
        // - BBBBB BBBBBB = Próximos 10 dígitos do campo livre
        // - CCCCC CCCCCC = Últimos 10 dígitos do campo livre
        // - D = Dígito verificador geral
        // - EEEEEEEEEEEEEE = Fator de vencimento (4) + Valor (10)
        
        if (barcode.length() < 44) {
            barcode = String.format("%-44s", barcode).replace(' ', '0');
        }
        
        // Monta os campos baseado na estrutura do código de barras
        // Barcode: BBB M DV FFFF VVVVVVVVVV CCCCCCCCCCCCCCCCCCCCCCCCC (44 dígitos)
        //          0-2 3 4  5-8  9-18       19-43
        
        String banco = barcode.substring(0, 3);     // 077
        String moeda = barcode.substring(3, 4);     // 9
        String dvGeral = barcode.substring(4, 5);   // DV
        String fatorValor = barcode.substring(5, 19); // Fator + Valor (14 dígitos)
        String campoLivre = barcode.substring(19, 44); // 25 dígitos
        
        // Campo 1: Banco (3) + Moeda (1) + Primeiros 5 dígitos do campo livre (Total: 9 dígitos)
        String campo1Base = banco + moeda + campoLivre.substring(0, 5);
        String dv1 = calculateMockDV(campo1Base);
        
        // Campo 2: Próximos 10 dígitos do campo livre
        String campo2Base = campoLivre.substring(5, 15);
        String dv2 = calculateMockDV(campo2Base);
        
        // Campo 3: Últimos 10 dígitos do campo livre
        String campo3Base = campoLivre.substring(15, 25);
        String dv3 = calculateMockDV(campo3Base);
        
        // Formata a linha digitável: XXXXX.XXXXX XXXXX.XXXXXX XXXXX.XXXXXX D EEEEEEEEEEEEEE
        return String.format("%s.%s%s %s.%s%s %s.%s%s %s %s",
            campo1Base.substring(0, 5),
            campo1Base.substring(5, 9),
            dv1,
            campo2Base.substring(0, 5),
            campo2Base.substring(5, 10),
            dv2,
            campo3Base.substring(0, 5),
            campo3Base.substring(5, 10),
            dv3,
            dvGeral,
            fatorValor
        );
    }
    
    private String generateMockPdfUrl(String nossoNumero) {
        return String.format("https://mock-banco-inter.test/api/boleto/pdf/%s", nossoNumero);
    }
    
    private String buildMockJsonResponse(String nossoNumero, String barcode, 
                                         String digitableLine, String pdfUrl,
                                         BoletoRequest request) {
        return String.format("""
            {
              "nossoNumero": "%s",
              "codigoBarras": "%s",
              "linhaDigitavel": "%s",
              "pdfBoleto": "%s",
              "dataEmissao": "%s",
              "dataVencimento": "%s",
              "valorNominal": %.2f,
              "pagador": {
                "cpfCnpj": "%s",
                "nome": "%s",
                "telefone": "%s"
              },
              "mock": true,
              "ambiente": "teste"
            }
            """,
            nossoNumero,
            barcode,
            digitableLine,
            pdfUrl,
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            request.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
            request.getAmount(),
            request.getPayerDocument(),
            request.getPayerName(),
            request.getPayerPhone() != null ? request.getPayerPhone() : ""
        );
    }
    
    private String calculateFatorVencimento(LocalDate dueDate) {
        // Fator de vencimento: dias desde 07/10/1997
        LocalDate baseDate = LocalDate.of(1997, 10, 7);
        long days = java.time.temporal.ChronoUnit.DAYS.between(baseDate, dueDate);
        return String.format("%04d", days % 10000);
    }
    
    private String formatValor(double valor) {
        // Valor em centavos com 10 dígitos
        long centavos = Math.round(valor * 100);
        return String.format("%010d", centavos);
    }
    
    private String calculateMockDV(String campo) {
        // DV simplificado usando módulo 11 (apenas para mock)
        int soma = 0;
        int multiplicador = 2;
        
        for (int i = campo.length() - 1; i >= 0; i--) {
            soma += Character.getNumericValue(campo.charAt(i)) * multiplicador;
            multiplicador = multiplicador == 9 ? 2 : multiplicador + 1;
        }
        
        int resto = soma % 11;
        int dv = 11 - resto;
        
        if (dv == 0 || dv == 10 || dv == 11) {
            dv = 1;
        }
        
        return String.valueOf(dv);
    }
}
