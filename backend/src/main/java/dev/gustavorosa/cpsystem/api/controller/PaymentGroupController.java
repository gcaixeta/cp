package dev.gustavorosa.cpsystem.api.controller;

import dev.gustavorosa.cpsystem.api.request.CreatePaymentGroupRequest;
import dev.gustavorosa.cpsystem.api.response.PaymentGroupResponse;
import dev.gustavorosa.cpsystem.service.PaymentGroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("v1/payment-group")
@Slf4j
public class PaymentGroupController {

    @Autowired
    private PaymentGroupService paymentGroupService;

    @GetMapping
    public List<PaymentGroupResponse> findAllPaymentGroups(@RequestParam(required = false) Long clientId) {
        log.info("[Entry - PaymentGroupController.findAllPaymentGroups] - Listing payment groups, clientId={}", clientId);
        List<PaymentGroupResponse> groups = paymentGroupService.findAllPaymentGroups(clientId);
        log.info("[Exit - PaymentGroupController.findAllPaymentGroups] - Found {} payment groups", groups.size());
        return groups;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentGroup(@PathVariable Long id) {
        log.info("[Entry - PaymentGroupController.deletePaymentGroup] - Deleting payment group with id: {}", id);
        paymentGroupService.deletePaymentGroup(id);
        log.info("[Exit - PaymentGroupController.deletePaymentGroup] - Payment group deleted successfully");
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<Void> createPaymentGroup(@RequestBody CreatePaymentGroupRequest request) {
        log.info("[Entry - PaymentGroupController.createPaymentGroup] - Creating payment group: {}", request);
        paymentGroupService.createPaymentGroup(request);
        log.info("[Exit - PaymentGroupController.createPaymentGroup] - Payment group created successfully");
        return ResponseEntity.ok().build();
    }
}
