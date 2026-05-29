package com.thluxury.payment.gateway;

import com.thluxury.payment.domain.Payment;

import java.math.BigDecimal;
import java.util.UUID;

/** Strategy interface — Mock cho dev, Stripe khi STRIPE_ENABLED=true. */
public interface PaymentGateway {

    String name();

    boolean supports(Payment.Method method);

    ChargeResult charge(ChargeCommand cmd);

    RefundResult refund(RefundCommand cmd);

    record ChargeCommand(UUID orderId, Payment.Method method,
                         BigDecimal amount, String currency,
                         String customerId, String clientToken,
                         String idempotencyKey) {}

    record ChargeResult(Payment.Status status, String gatewayRef,
                        String errorCode, String errorMessage) {}

    record RefundCommand(String paymentId, String gatewayRef,
                         BigDecimal amount, String reason) {}

    record RefundResult(boolean success, Payment.Status newStatus,
                        String errorCode, String errorMessage) {}
}
