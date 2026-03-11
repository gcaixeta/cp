package dev.gustavorosa.cpsystem.boleto.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.gustavorosa.cpsystem.boleto.dto.BankApiResponse;
import dev.gustavorosa.cpsystem.boleto.dto.BoletoRequest;
import dev.gustavorosa.cpsystem.boleto.exception.BoletoGenerationException;
import dev.gustavorosa.cpsystem.boleto.model.BankType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@ConditionalOnProperty(name = "bank.mock.enabled", havingValue = "false", matchIfMissing = true)
@Component
public class InterBoletoStrategy implements BankBoletoStrategy {
    
    @Value("${bank.inter.api.url}")
    private String apiUrl;
    
    @Value("${bank.inter.api.oauth-url}")
    private String oauthUrl;
    
    @Value("${bank.inter.client.id}")
    private String clientId;
    
    @Value("${bank.inter.client.secret}")
    private String clientSecret;
    
    @Value("${bank.inter.scopes}")
    private String scopes;
    
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    
    private String cachedAccessToken;
    private long tokenExpirationTime;
    
    public InterBoletoStrategy(
        @Qualifier("interRestClient") RestClient restClient,
        ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public BankApiResponse generateBoleto(BoletoRequest request) {
        try {
            log.info("Gerando boleto no Banco Inter para payment {}", request.getPaymentId());
            
            // 1. Obter token OAuth2
            String accessToken = getAccessToken();
            
            // 2. Montar payload específico do Banco Inter
            Map<String, Object> payload = buildInterPayload(request);
            
            // 3. Fazer requisição para API do Inter
            String responseBody = restClient.post()
                .uri(apiUrl + "/cobranca/v3/cobrancas")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(payload)
                .retrieve()
                .body(String.class);
            
            // 4. Processar resposta
            return parseInterResponse(responseBody);
            
        } catch (Exception e) {
            log.error("Erro ao gerar boleto no Banco Inter para payment {}", request.getPaymentId(), e);
            return BankApiResponse.builder()
                .success(false)
                .errorMessage("Erro ao gerar boleto: " + e.getMessage())
                .build();
        }
    }
    
    private String getAccessToken() {
        // Verificar se o token em cache ainda é válido
        if (cachedAccessToken != null && System.currentTimeMillis() < tokenExpirationTime) {
            return cachedAccessToken;
        }
        
        try {
            log.info("Obtendo novo token OAuth2 do Banco Inter");
            
            // Criar credenciais Basic Auth
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
            
            // Montar corpo da requisição
            Map<String, String> body = new HashMap<>();
            body.put("grant_type", "client_credentials");
            body.put("scope", scopes);
            
            // Fazer requisição OAuth2
            String responseBody = restClient.post()
                .uri(oauthUrl)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedCredentials)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(convertMapToUrlEncoded(body))
                .retrieve()
                .body(String.class);
            
            // Parsear resposta
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            cachedAccessToken = jsonResponse.get("access_token").asText();
            int expiresIn = jsonResponse.get("expires_in").asInt();
            
            // Renovar token 5 minutos antes de expirar
            tokenExpirationTime = System.currentTimeMillis() + ((expiresIn - 300) * 1000L);
            
            log.info("Token OAuth2 obtido com sucesso. Válido por {} segundos", expiresIn);
            return cachedAccessToken;
            
        } catch (Exception e) {
            log.error("Erro ao obter token OAuth2 do Banco Inter", e);
            throw new BoletoGenerationException("Erro ao autenticar com Banco Inter", e);
        }
    }
    
    private Map<String, Object> buildInterPayload(BoletoRequest request) {
        Map<String, Object> payload = new HashMap<>();
        
        // Número do documento (identificador único)
        payload.put("seuNumero", String.valueOf(request.getPaymentId()));
        
        // Valor nominal
        payload.put("valorNominal", request.getAmount());
        
        // Data de vencimento (formato YYYY-MM-DD)
        payload.put("dataVencimento", request.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        // Dias para baixa automática (após vencimento)
        payload.put("numDiasAgenda", "SESSENTA");
        
        // Dados do pagador
        Map<String, Object> pagador = new HashMap<>();
        pagador.put("cpfCnpj", request.getPayerDocument().replaceAll("\\D", ""));
        pagador.put("nome", request.getPayerName());
        if (request.getPayerPhone() != null && !request.getPayerPhone().isEmpty()) {
            pagador.put("telefone", request.getPayerPhone().replaceAll("\\D", ""));
        }
        payload.put("pagador", pagador);
        
        // Mensagem no boleto
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            Map<String, String> mensagem = new HashMap<>();
            mensagem.put("linha1", request.getDescription());
            payload.put("mensagem", mensagem);
        }
        
        // Multa (se configurada)
        if (request.getLateFeeRate() != null && request.getLateFeeRate().compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> multa = new HashMap<>();
            multa.put("codigo", "PERCENTUAL");
            multa.put("taxa", request.getLateFeeRate().multiply(BigDecimal.valueOf(100))); // Converter para percentual
            multa.put("data", request.getDueDate().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
            payload.put("multa", multa);
        }
        
        // Juros/Mora (se configurado)
        if (request.getMonthlyInterestRate() != null && request.getMonthlyInterestRate().compareTo(BigDecimal.ZERO) > 0) {
            Map<String, Object> mora = new HashMap<>();
            mora.put("codigo", "TAXAMENSAL");
            mora.put("taxa", request.getMonthlyInterestRate().multiply(BigDecimal.valueOf(100))); // Converter para percentual
            mora.put("data", request.getDueDate().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
            payload.put("mora", mora);
        }
        
        return payload;
    }
    
    private BankApiResponse parseInterResponse(String responseBody) {
        try {
            JsonNode json = objectMapper.readTree(responseBody);
            
            return BankApiResponse.builder()
                .success(true)
                .bankBoletoId(json.get("nossoNumero").asText())
                .barcode(json.has("codigoBarras") ? json.get("codigoBarras").asText() : null)
                .digitableLine(json.has("linhaDigitavel") ? json.get("linhaDigitavel").asText() : null)
                .pdfUrl(json.has("pdfBoleto") ? json.get("pdfBoleto").asText() : null)
                .rawResponse(responseBody)
                .build();
                
        } catch (Exception e) {
            log.error("Erro ao parsear resposta do Banco Inter", e);
            throw new BoletoGenerationException("Erro ao processar resposta do banco", e);
        }
    }
    
    private String convertMapToUrlEncoded(Map<String, String> map) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (result.length() > 0) {
                result.append("&");
            }
            result.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return result.toString();
    }
    
    @Override
    public BankType getSupportedBank() {
        return BankType.INTER;
    }
}
