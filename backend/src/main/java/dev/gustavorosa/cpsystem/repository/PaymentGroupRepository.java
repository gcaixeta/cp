package dev.gustavorosa.cpsystem.repository;

import dev.gustavorosa.cpsystem.model.PaymentGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentGroupRepository extends JpaRepository<PaymentGroup, Long> {
    long countByPayerDocument(String payerDocument);
    List<PaymentGroup> findByClientId(Long clientId);
}
