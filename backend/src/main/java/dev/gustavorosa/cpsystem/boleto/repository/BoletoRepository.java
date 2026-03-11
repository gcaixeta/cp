package dev.gustavorosa.cpsystem.boleto.repository;

import dev.gustavorosa.cpsystem.boleto.model.Boleto;
import dev.gustavorosa.cpsystem.boleto.model.BoletoStatus;
import dev.gustavorosa.cpsystem.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoletoRepository extends JpaRepository<Boleto, Long> {
    Optional<Boleto> findByPayment(Payment payment);
    boolean existsByPayment(Payment payment);
    List<Boleto> findByStatus(BoletoStatus status);
}
