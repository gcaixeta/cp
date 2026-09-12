package dev.gustavorosa.cpsystem.model;

import dev.gustavorosa.cpsystem.api.response.PaymentResponse;
import dev.gustavorosa.cpsystem.utils.NameUtils;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_group")
    private PaymentGroup paymentGroup;

    @Column(name = "payer_name", nullable = false, length = 50)
    private String payerName;

    @Column(name = "payer_document", length = 14)
    private String payerDocument;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "total_installments", nullable = false)
    private Integer totalInstallments;

    @Column(name = "original_value", nullable = false)
    private BigDecimal originalValue;

    @Column(name = "overdue_value")
    private BigDecimal overdueValue;

    @Column(name = "overdue_value_date")
    private LocalDate overdueValueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Column(length = 400)
    private String observation;

    public PaymentResponse toResponse() {
        return new PaymentResponse(
                this.getId(),
                this.getClient().getId(),
                this.getPaymentGroup().getId(),
                this.getPaymentGroup().getGroupName(),
                this.getPayerName(),
                this.getPaymentGroup().getPayerPhone(),
                this.getInstallmentNumber(),
                this.getTotalInstallments(),
                this.getOriginalValue(),
                this.getOverdueValue(),
                this.getDueDate(),
                this.getPaymentDate(),
                this.getPaymentStatus(),
                this.getObservation()
        );
    }

    @PrePersist
    @PreUpdate
    void normalizePayerName() {
        payerName = NameUtils.toTitleCase(payerName);
    }
}
