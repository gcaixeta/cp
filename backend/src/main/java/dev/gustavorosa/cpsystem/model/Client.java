package dev.gustavorosa.cpsystem.model;

import dev.gustavorosa.cpsystem.utils.NameUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String address;

    @Column(length = 15)
    private String phone;

    @Column(nullable = false, length = 14, unique = true)
    private String document;

    @Column(length = 100)
    private String bank;

    @Column(name = "late_fee_rate", precision = 10, scale = 4)
    private BigDecimal lateFeeRate;

    @Column(name = "monthly_interest_rate", precision = 10, scale = 4)
    private BigDecimal monthlyInterestRate;


    @PrePersist
    @PreUpdate
    void normalizeName() {
        name = NameUtils.toTitleCase(name);
    }
}
