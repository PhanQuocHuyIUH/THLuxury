package com.thluxury.payment.web;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.thluxury.payment.domain.Payment;
import com.thluxury.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nhận webhook từ Stripe. Chỉ bật khi STRIPE_ENABLED=true.
 * Path đã được permitAll trong SecurityConfig: /api/payments/stripe/webhook
 *
 * Test bằng Stripe CLI:
 *   stripe listen --forward-to localhost:8085/api/payments/stripe/webhook
 *   (copy whsec_... vào STRIPE_WEBHOOK_SECRET rồi restart payment-service)
 */
@RestController
@ConditionalOnProperty(name = "thluxury.payment.stripe.enabled", havingValue = "true")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final PaymentService payments;
    private final String webhookSecret;

    public StripeWebhookController(PaymentService payments,
                                   @Value("${thluxury.payment.stripe.webhook-secret:}") String webhookSecret) {
        this.payments = payments;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/api/payments/stripe/webhook")
    public ResponseEntity<String> handle(@RequestBody String payload,
                                         @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Webhook nhận được nhưng STRIPE_WEBHOOK_SECRET chưa cấu hình — bỏ qua verify");
            return ResponseEntity.status(503).body("webhook secret not configured");
        }
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook signature không hợp lệ: {}", e.getMessage());
            return ResponseEntity.badRequest().body("invalid signature");
        }

        PaymentIntent pi = extractPaymentIntent(event);
        if (pi == null) {
            return ResponseEntity.ok("ignored:" + event.getType());
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" ->
                    payments.confirmFromWebhook(pi.getId(), Payment.Status.SUCCESS, null);
            case "payment_intent.payment_failed" ->
                    payments.confirmFromWebhook(pi.getId(), Payment.Status.FAILED,
                            pi.getLastPaymentError() != null ? pi.getLastPaymentError().getCode() : "STRIPE_FAILED");
            default -> log.debug("Webhook bỏ qua event type {}", event.getType());
        }
        return ResponseEntity.ok("ok");
    }

    private PaymentIntent extractPaymentIntent(Event event) {
        StripeObject obj = event.getDataObjectDeserializer().getObject().orElse(null);
        return (obj instanceof PaymentIntent pi) ? pi : null;
    }
}
