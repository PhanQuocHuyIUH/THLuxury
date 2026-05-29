package com.thluxury.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thluxury.payment.domain.Payment;
import com.thluxury.payment.domain.PaymentEvent;
import com.thluxury.payment.gateway.PaymentGateway;
import com.thluxury.payment.gateway.PaymentGatewayResolver;
import com.thluxury.payment.messaging.PaymentEventPublisher;
import com.thluxury.payment.repository.PaymentEventRepository;
import com.thluxury.payment.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository payments;
    private final PaymentEventRepository events;
    private final PaymentGatewayResolver resolver;
    private final PaymentEventPublisher publisher;
    private final ObjectMapper json;
    private final MeterRegistry metrics;

    public PaymentService(PaymentRepository payments,
                          PaymentEventRepository events,
                          PaymentGatewayResolver resolver,
                          PaymentEventPublisher publisher,
                          ObjectMapper json,
                          MeterRegistry metrics) {
        this.payments = payments;
        this.events = events;
        this.resolver = resolver;
        this.publisher = publisher;
        this.json = json;
        this.metrics = metrics;
    }

    public record ChargeInput(UUID orderId, Payment.Method method,
                              BigDecimal amount, String currency,
                              String customerId, String clientToken,
                              String idempotencyKey) {}

    public record ChargeOutcome(Payment payment, String errorCode, String errorMessage) {}

    @Transactional
    public ChargeOutcome charge(ChargeInput in) {
        // Idempotency: nếu đã có payment với key cũ → trả lại y nguyên.
        if (in.idempotencyKey() != null) {
            var existing = payments.findByIdempotencyKey(in.idempotencyKey());
            if (existing.isPresent()) {
                Payment p = existing.get();
                log.info("Idempotent hit on key={} → reuse payment {}", in.idempotencyKey(), p.getId());
                return new ChargeOutcome(p, null, null);
            }
        }

        PaymentGateway gw = resolver.resolve(in.method());

        Payment p = new Payment();
        p.setOrderId(in.orderId());
        p.setProvider(Payment.Provider.valueOf(gw.name()));
        p.setMethod(in.method());
        p.setAmount(in.amount());
        p.setCurrency(in.currency() == null ? "VND" : in.currency());
        p.setIdempotencyKey(in.idempotencyKey());
        p.setAttemptCount(1);
        p = payments.save(p);

        appendEvent(p.getId(), "CHARGE_INITIATED", Map.of(
                "orderId", in.orderId().toString(),
                "method", in.method().name(),
                "amount", in.amount().toPlainString(),
                "gateway", gw.name()));

        PaymentGateway.ChargeResult res = gw.charge(new PaymentGateway.ChargeCommand(
                in.orderId(), in.method(), in.amount(), p.getCurrency(),
                in.customerId(), in.clientToken(), in.idempotencyKey()));

        p.setStatus(res.status());
        p.setGatewayRef(res.gatewayRef());
        if (res.errorMessage() != null) p.setErrorMessage(res.errorMessage());
        payments.save(p);

        switch (res.status()) {
            case SUCCESS -> {
                appendEvent(p.getId(), "CHARGE_SUCCESS", Map.of("gatewayRef", safe(res.gatewayRef())));
                publisher.publishSucceeded(p);
            }
            case FAILED -> {
                appendEvent(p.getId(), "CHARGE_FAILED", Map.of(
                        "errorCode", safe(res.errorCode()),
                        "errorMessage", safe(res.errorMessage())));
                publisher.publishFailed(p, res.errorCode());
            }
            case PENDING -> appendEvent(p.getId(), "CHARGE_PENDING", Map.of(
                    "gatewayRef", safe(res.gatewayRef())));
            default -> { /* no-op */ }
        }

        metrics.counter("payment.outcome",
                "provider", p.getProvider().name(),
                "method", in.method().name(),
                "result", res.status().name()).increment();

        return new ChargeOutcome(p, res.errorCode(), res.errorMessage());
    }

    @Transactional
    public PaymentGateway.RefundResult refund(UUID paymentId, BigDecimal amount, String reason) {
        Payment p = payments.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("PAYMENT_NOT_FOUND"));
        if (p.getStatus() != Payment.Status.SUCCESS) {
            throw new IllegalStateException("Cannot refund payment in status " + p.getStatus());
        }
        PaymentGateway gw = resolver.resolve(p.getMethod());
        PaymentGateway.RefundResult r = gw.refund(new PaymentGateway.RefundCommand(
                p.getId().toString(), p.getGatewayRef(),
                amount == null ? p.getAmount() : amount, reason));
        if (r.success()) {
            p.setStatus(Payment.Status.REFUNDED);
            payments.save(p);
            appendEvent(p.getId(), "REFUNDED", Map.of(
                    "reason", reason == null ? "" : reason,
                    "amount", (amount == null ? p.getAmount() : amount).toPlainString()));
        } else {
            appendEvent(p.getId(), "REFUND_FAILED", Map.of(
                    "errorCode", safe(r.errorCode()),
                    "errorMessage", safe(r.errorMessage())));
        }
        return r;
    }

    /**
     * Cập nhật payment từ Stripe webhook (payment_intent.succeeded/failed).
     * Idempotent: nếu trạng thái đã đúng thì bỏ qua. Tìm payment theo gatewayRef (PaymentIntent id).
     */
    @Transactional
    public void confirmFromWebhook(String gatewayRef, Payment.Status status, String errorCode) {
        if (gatewayRef == null || gatewayRef.isBlank()) return;
        payments.findByGatewayRef(gatewayRef).ifPresentOrElse(p -> {
            if (p.getStatus() == status) {
                log.info("Webhook idempotent: payment {} đã ở trạng thái {}", p.getId(), status);
                return;
            }
            p.setStatus(status);
            payments.save(p);
            if (status == Payment.Status.SUCCESS) {
                appendEvent(p.getId(), "CHARGE_SUCCESS_WEBHOOK", Map.of("gatewayRef", gatewayRef));
                publisher.publishSucceeded(p);
            } else if (status == Payment.Status.FAILED) {
                appendEvent(p.getId(), "CHARGE_FAILED_WEBHOOK", Map.of(
                        "gatewayRef", gatewayRef, "errorCode", safe(errorCode)));
                publisher.publishFailed(p, errorCode);
            }
            log.info("Webhook cập nhật payment {} (PI {}) → {}", p.getId(), gatewayRef, status);
        }, () -> log.warn("Webhook: không tìm thấy payment cho PaymentIntent {}", gatewayRef));
    }

    private void appendEvent(UUID paymentId, String type, Map<String, ?> payload) {
        try {
            events.save(new PaymentEvent(paymentId, type, json.writeValueAsString(payload)));
        } catch (Exception e) {
            log.warn("Failed to append event {} for {}: {}", type, paymentId, e.getMessage());
        }
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
