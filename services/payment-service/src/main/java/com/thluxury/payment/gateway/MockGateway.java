package com.thluxury.payment.gateway;

import com.thluxury.payment.domain.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MockGateway:
 *  - COD: luôn return PENDING (chỉ tạo record, không "thu" gì).
 *  - BANK_TRANSFER / CREDIT_CARD: sleep random(0, MAX_DELAY) rồi
 *    với xác suất failure-rate → trả FAILED, ngược lại SUCCESS.
 */
@Component
public class MockGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockGateway.class);

    private final double failureRate;
    private final long maxDelayMs;

    public MockGateway(@Value("${thluxury.payment.mock.failure-rate:0.2}") double failureRate,
                       @Value("${thluxury.payment.mock.max-delay-ms:1500}") long maxDelayMs) {
        this.failureRate = failureRate;
        this.maxDelayMs = maxDelayMs;
    }

    @Override
    public String name() { return "MOCK"; }

    @Override
    public boolean supports(Payment.Method method) { return true; }

    @Override
    public ChargeResult charge(ChargeCommand cmd) {
        if (cmd.method() == Payment.Method.COD) {
            return new ChargeResult(Payment.Status.PENDING,
                    "mock-cod-" + UUID.randomUUID(), null, null);
        }
        try {
            long delay = ThreadLocalRandom.current().nextLong(0, Math.max(1, maxDelayMs));
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
        double dice = ThreadLocalRandom.current().nextDouble();
        if (dice < failureRate) {
            log.warn("Mock charge FAILED (dice={} < failureRate={}) order={}", dice, failureRate, cmd.orderId());
            return new ChargeResult(Payment.Status.FAILED, null,
                    "MOCK_DECLINED", "Mock gateway declined (simulated failure)");
        }
        return new ChargeResult(Payment.Status.SUCCESS,
                "mock-ok-" + UUID.randomUUID(), null, null);
    }

    @Override
    public RefundResult refund(RefundCommand cmd) {
        return new RefundResult(true, Payment.Status.REFUNDED, null, null);
    }
}
