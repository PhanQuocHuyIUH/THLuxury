package com.thluxury.order.grpc;

import com.thluxury.proto.common.Money;
import com.thluxury.proto.common.OrderRef;
import com.thluxury.proto.payment.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class PaymentGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentGrpcClient.class);

    @GrpcClient("payment")
    private PaymentServiceGrpc.PaymentServiceBlockingStub stub;

    public record ChargeOutcome(String paymentId, String status, String gatewayRef,
                                String errorCode, String errorMessage) {}

    @CircuitBreaker(name = "payment", fallbackMethod = "chargeFallback")
    @Retry(name = "payment")
    public ChargeOutcome charge(UUID orderId, String maDh, String method,
                                BigDecimal amount, String customerId, String idempotencyKey) {
        ChargeRequest req = ChargeRequest.newBuilder()
                .setOrder(OrderRef.newBuilder()
                        .setOrderId(orderId.toString())
                        .setMaDh(maDh == null ? "" : maDh)
                        .build())
                .setAmount(Money.newBuilder()
                        .setAmount(amount.longValue())
                        .setCurrency("VND")
                        .build())
                .setMethod(mapMethod(method))
                .setCustomerId(customerId == null ? "" : customerId)
                .setIdempotencyKey(idempotencyKey == null ? "" : idempotencyKey)
                .build();
        ChargeResponse resp = stub.charge(req);
        return new ChargeOutcome(
                resp.getPaymentId(),
                resp.getStatus().name(),
                resp.getGatewayRef(),
                resp.hasError() ? resp.getError().getCode() : null,
                resp.hasError() ? resp.getError().getMessage() : null);
    }

    @SuppressWarnings("unused")
    private ChargeOutcome chargeFallback(UUID orderId, String maDh, String method,
                                         BigDecimal amount, String customerId,
                                         String idempotencyKey, Throwable t) {
        log.warn("CircuitBreaker fallback: charge({}) failed → {}", maDh, t.toString());
        return new ChargeOutcome(null, "FAILED", null,
                "PAYMENT_UNAVAILABLE",
                "Payment service tạm thời không khả dụng — vui lòng thử lại sau.");
    }

    private static PaymentMethod mapMethod(String method) {
        if (method == null) return PaymentMethod.COD;
        return switch (method.toUpperCase()) {
            case "COD" -> PaymentMethod.COD;
            case "BANK_TRANSFER" -> PaymentMethod.BANK_TRANSFER;
            case "CREDIT_CARD" -> PaymentMethod.CREDIT_CARD;
            default -> PaymentMethod.COD;
        };
    }
}
