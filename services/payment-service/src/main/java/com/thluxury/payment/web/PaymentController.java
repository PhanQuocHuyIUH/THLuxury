package com.thluxury.payment.web;

import com.thluxury.payment.domain.Payment;
import com.thluxury.payment.repository.PaymentRepository;
import com.thluxury.payment.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository repo;
    private final PaymentService service;

    public PaymentController(PaymentRepository repo, PaymentService service) {
        this.repo = repo;
        this.service = service;
    }

    public record PaymentView(UUID id, UUID orderId, String provider, String method,
                              BigDecimal amount, String currency, String status,
                              String gatewayRef, int attemptCount, String errorMessage,
                              Instant createdAt) {}

    public record RefundRequest(BigDecimal amount, String reason) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public List<PaymentView> list(@RequestParam(required = false) UUID orderId) {
        var list = orderId == null ? repo.findAll() : repo.findByOrderId(orderId);
        return list.stream().map(this::toView).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public PaymentView get(@PathVariable UUID id) {
        return repo.findById(id).map(this::toView).orElseThrow();
    }

    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> refund(@PathVariable UUID id, @RequestBody RefundRequest req) {
        service.refund(id, req == null ? null : req.amount(), req == null ? null : req.reason());
        return ResponseEntity.ok().build();
    }

    private PaymentView toView(Payment p) {
        return new PaymentView(p.getId(), p.getOrderId(), p.getProvider().name(), p.getMethod().name(),
                p.getAmount(), p.getCurrency(), p.getStatus().name(), p.getGatewayRef(),
                p.getAttemptCount(), p.getErrorMessage(), p.getCreatedAt());
    }
}
