package dev.gustavorosa.cpsystem.security;

import dev.gustavorosa.cpsystem.security.dto.AuthResponse;
import dev.gustavorosa.cpsystem.security.dto.LoginRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("v1/auth")
public class AuthController {

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Value("${auth.user.email}")
    private String validEmail;

    @Value("${auth.user.password}")
    private String validPassword;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("[Entry - AuthController.login] - Logging in user: {}", loginRequest);
        
        // Validate credentials against environment variables
        if (!validEmail.equals(loginRequest.getEmail()) || 
            !validPassword.equals(loginRequest.getPassword())) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Credenciais inválidas");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Generate JWT token
        String token = tokenProvider.generateToken(loginRequest.getEmail());
        
        AuthResponse response = new AuthResponse(token, tokenProvider.getExpirationMs());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate() {
        // This endpoint requires authentication, so if we reach here, token is valid
        return ResponseEntity.ok().body(Map.of("valid", true));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token não fornecido"));
        }

        String expiredToken = authHeader.substring(7);
        String email = tokenProvider.getEmailFromExpiredToken(expiredToken);

        if (email == null || !email.equals(validEmail)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Token inválido para refresh"));
        }

        String newToken = tokenProvider.generateToken(email);
        return ResponseEntity.ok(new AuthResponse(newToken, tokenProvider.getExpirationMs()));
    }
}
