package dev.gustavorosa.cpsystem.api.controller;

import dev.gustavorosa.cpsystem.api.request.UpdatePaymentRequest;
import dev.gustavorosa.cpsystem.api.response.GroupedPaymentResponse;
import dev.gustavorosa.cpsystem.api.response.PaymentResponse;
import dev.gustavorosa.cpsystem.model.PaymentStatus;
import dev.gustavorosa.cpsystem.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/payment")
@Slf4j
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @GetMapping("/all")
    public List<PaymentResponse> findAllWithNoFilter() {
        return paymentService.findAllPayment();
    }

    @GetMapping
    public List<GroupedPaymentResponse> findAllPayments(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        log.info("[Entry - PaymentController.findAllPayments] - Searching for all payments with filters: clientId={}, status={}, month={}, year={}", clientId, status, month, year);
        List<GroupedPaymentResponse> payments = paymentService.findGroupedPayments(clientId, status, month, year);
        log.info("[Exit - PaymentController.findAllPayments] - Found {} grouped payments", payments.size());
        return payments;
    }

    @PutMapping("/{id}")
    public PaymentResponse updatePayment(@PathVariable Long id, @RequestBody UpdatePaymentRequest request) {
        log.info("[Entry - PaymentController.updatePayment] - Updating payment with id: {}, request: {}", id, request);
        PaymentResponse updatedPayment = paymentService.updatePayment(id, request);
        log.info("[Exit - PaymentController.updatePayment] - Payment updated successfully: {}", updatedPayment);
        return updatedPayment;
    }

    @PatchMapping("/{id}/mark-as-paid")
    public PaymentResponse markAsPaid(@PathVariable Long id) {
        log.info("[Entry - PaymentController.markAsPaid] - Marking payment {} as paid", id);
        PaymentResponse updatedPayment = paymentService.markAsPaid(id);
        log.info("[Exit - PaymentController.markAsPaid] - Payment marked as paid successfully: {}", updatedPayment);
        return updatedPayment;
    }

    @PatchMapping("/{id}/revert-to-pending")
    public PaymentResponse revertToPending(@PathVariable Long id) {
        log.info("[Entry - PaymentController.revertToPending] - Reverting payment {} to pending", id);
        PaymentResponse updatedPayment = paymentService.revertToPending(id);
        log.info("[Exit - PaymentController.revertToPending] - Payment reverted to pending: {}", updatedPayment);
        return updatedPayment;
    }
}
