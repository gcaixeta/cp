package dev.gustavorosa.cpsystem.boleto.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

@Slf4j
@Configuration
@ConditionalOnProperty(name = "bank.mock.enabled", havingValue = "false", matchIfMissing = true)
public class InterRestClientConfig {
    
    @Value("${bank.inter.certificate.path}")
    private Resource certificatePath;
    
    @Value("${bank.inter.certificate.password}")
    private String certificatePassword;
    
    @Bean(name = "interRestClient")
    public RestClient interRestClient() {
        try {
            SSLContext sslContext = createSSLContext();
            ClientHttpRequestFactory requestFactory = createRequestFactory(sslContext);
            
            return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
        } catch (Exception e) {
            log.error("Erro ao configurar RestClient para Banco Inter", e);
            // Retorna um RestClient padrão caso o certificado não esteja configurado
            return RestClient.builder().build();
        }
    }
    
    private SSLContext createSSLContext() throws Exception {
        if (certificatePath == null || !certificatePath.exists()) {
            log.warn("Certificado do Banco Inter não encontrado. Usando configuração padrão.");
            return SSLContext.getDefault();
        }
        
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream inputStream = certificatePath.getInputStream()) {
            keyStore.load(inputStream, certificatePassword.toCharArray());
        }
        
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, certificatePassword.toCharArray());
        
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
        
        return sslContext;
    }
    
    private ClientHttpRequestFactory createRequestFactory(SSLContext sslContext) throws IOException {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(30000); // 30 seconds
        
        if (sslContext != null) {
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        }
        
        return factory;
    }
}
