package dev.gustavorosa.cpsystem.repository;

import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentStatus;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    @Modifying
    @Transactional
    @Query(
            """
            UPDATE Payment p
            SET p.paymentStatus = dev.gustavorosa.cpsystem.model.PaymentStatus.OVERDUE
            WHERE p.dueDate < CURRENT_DATE
            AND p.paymentStatus = dev.gustavorosa.cpsystem.model.PaymentStatus.PENDING
            """
    )
    int updatePaymentStatus();

    @Query("SELECT p FROM Payment p JOIN FETCH p.paymentGroup WHERE p.paymentStatus = dev.gustavorosa.cpsystem.model.PaymentStatus.OVERDUE")
    List<Payment> findAllOverdueWithGroup();

    List<Payment> findByPaymentStatus(PaymentStatus paymentStatus);

    List<Payment> findByPaymentGroupId(Long paymentGroupId);

    @Query(value = "SELECT * FROM payments p WHERE (CAST(:clientId AS TEXT) IS NULL OR p.client_id = :clientId) " +
           "AND (CAST(:status AS TEXT) IS NULL OR p.payment_status = :status) " +
           "AND (p.due_date BETWEEN :startDate AND :endDate)", nativeQuery = true)
    List<Payment> findFilteredPayments(
            @org.springframework.data.repository.query.Param("clientId") Long clientId,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("startDate") LocalDate startDate,
            @org.springframework.data.repository.query.Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM Payment p WHERE p.paymentGroup.id IN :groupIds " +
           "AND p.paymentStatus = dev.gustavorosa.cpsystem.model.PaymentStatus.OVERDUE")
    List<Payment> findOverduePaymentsByGroups(@org.springframework.data.repository.query.Param("groupIds") List<Long> groupIds);

    @Query(value = "SELECT * FROM payments p WHERE (CAST(:clientId AS TEXT) IS NULL OR p.client_id = :clientId) " +
           "AND p.payment_status = 'OVERDUE'", nativeQuery = true)
    List<Payment> findOverduePayments(@org.springframework.data.repository.query.Param("clientId") Long clientId);

    @Query("SELECT p FROM Payment p JOIN FETCH p.paymentGroup WHERE p.client.id = :clientId AND p.dueDate BETWEEN :startDate AND :endDate")
    List<Payment> findByClientIdAndDueDateBetween(
            @org.springframework.data.repository.query.Param("clientId") Long clientId,
            @org.springframework.data.repository.query.Param("startDate") LocalDate startDate,
            @org.springframework.data.repository.query.Param("endDate") LocalDate endDate);

    @Query("SELECT p FROM Payment p JOIN FETCH p.paymentGroup WHERE p.client.id = :clientId AND p.paymentDate BETWEEN :startDate AND :endDate")
    List<Payment> findByClientIdAndPaymentDateBetween(
            @org.springframework.data.repository.query.Param("clientId") Long clientId,
            @org.springframework.data.repository.query.Param("startDate") LocalDate startDate,
            @org.springframework.data.repository.query.Param("endDate") LocalDate endDate);
}
