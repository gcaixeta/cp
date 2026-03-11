package dev.gustavorosa.cpsystem.boleto.model;

import dev.gustavorosa.cpsystem.model.Payment;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "boletos")
public class Boleto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "bank_type", nullable = false)
    private BankType bankType;
    
    @Column(name = "bank_boleto_id", unique = true, length = 100)
    private String bankBoletoId;
    
    @Column(length = 54)
    private String barcode;
    
    @Column(name = "digitable_line", length = 54)
    private String digitableLine;
    
    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BoletoStatus status;
    
    @Column(name = "bank_api_response", columnDefinition = "TEXT")
    private String bankApiResponse;
    
    @Column(name = "error_message")
    private String errorMessage;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
