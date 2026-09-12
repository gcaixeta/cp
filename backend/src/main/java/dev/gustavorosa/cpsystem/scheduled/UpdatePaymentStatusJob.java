package dev.gustavorosa.cpsystem.scheduled;

import dev.gustavorosa.cpsystem.model.Payment;
import dev.gustavorosa.cpsystem.model.PaymentGroup;
import dev.gustavorosa.cpsystem.repository.PaymentRepository;
import dev.gustavorosa.cpsystem.service.OverdueCalculator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class UpdatePaymentStatusJob {

    @Autowired
    private PaymentRepository paymentRepository;

    @Scheduled(cron = "0 0 2 * * *", zone = "America/Sao_Paulo")
    public int updatePaymentStatus() {
        log.info("Starting update of payment status...");
        int affectedRows = paymentRepository.updatePaymentStatus();
        log.info("Update payment status job done. {} rows updated!", affectedRows);
        return affectedRows;
    }


    @Scheduled(cron = "0 30 2 * * *", zone = "America/Sao_Paulo")
    public int updatePaymentOverdueValues() {
        log.info("Starting update of overdue payment values...");
        int updatedPayments = 0;
        
        List<Payment> overduePayments = paymentRepository.findAllOverdueWithGroup();
        log.info("Found {} overdue payments to process", overduePayments.size());
        
        LocalDate today = LocalDate.now();
        long todayEpoch = today.toEpochDay();

        for (Payment payment : overduePayments) {
            try {
                PaymentGroup group = payment.getPaymentGroup();
                if (group == null) {
                    log.warn("Payment ID {} has no group, skipping value update", payment.getId());
                    continue;
                }

                BigDecimal originalValue = payment.getOriginalValue();
                long dueDateEpoch = payment.getDueDate().toEpochDay();
                long daysOverdue = todayEpoch - dueDateEpoch;

                if (daysOverdue <= 0) {
                    continue;
                }

                BigDecimal newOverdueValue = OverdueCalculator.calculateOverdueValue(
                        originalValue, group.getLateFeeRate(), group.getMonthlyInterestRate(), daysOverdue);

                // Só atualiza se o valor mudou ou se é uma nova data
                if (payment.getOverdueValue() == null ||
                    payment.getOverdueValue().compareTo(newOverdueValue) != 0 ||
                    payment.getOverdueValueDate() == null ||
                    !payment.getOverdueValueDate().isEqual(today)) {

                    payment.setOverdueValue(newOverdueValue);
                    payment.setOverdueValueDate(today);
                    updatedPayments++;

                    log.debug("Updated Payment ID {}: Original={}, DaysOverdue={}, NewTotal={}",
                        payment.getId(), originalValue, daysOverdue, newOverdueValue);
                }
            } catch (Exception e) {
                log.error("Error updating overdue value for payment ID {}: {}", payment.getId(), e.getMessage());
            }
        }

        if (updatedPayments > 0) {
            paymentRepository.saveAll(overduePayments);
        }
        log.info("Finished updating values for {} payments.", updatedPayments);
        return updatedPayments;
    }
}
