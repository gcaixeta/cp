package dev.gustavorosa.cpsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@EnableScheduling
public class CpsystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(CpsystemApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {
			@Override
			public void addCorsMappings(CorsRegistry registry) {
				// Obter origins permitidos de variável de ambiente
				String allowedOrigins = System.getenv().getOrDefault(
					"ALLOWED_ORIGINS",
					"http://localhost:3000,http://localhost,http://localhost:80"
				);
				
				registry.addMapping("/**")
						.allowedOrigins(allowedOrigins.split(","))
						.allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
						.allowedHeaders("*")
						.allowCredentials(true);
			}
		};
	}

}
