package com.thluxury.payment.gateway;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import com.thluxury.payment.domain.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * StripeGateway THẬT — kích hoạt khi STRIPE_ENABLED=true.
 *
 * Luồng demo (server-side confirm):
 *  - Tạo PaymentIntent với amount/currency, confirm ngay bằng test PaymentMethod
 *    (mặc định "pm_card_visa") → ở test mode trả "succeeded" đồng bộ.
 *  - clientToken (nếu FE dùng Stripe.js Elements tạo PaymentMethod) sẽ được ưu tiên.
 *  - Webhook payment_intent.succeeded/failed là nguồn xác nhận chính thức (idempotent).
 *
 * VND là zero-decimal currency của Stripe → amount gửi nguyên giá trị (không *100).
 */
@Component
@ConditionalOnProperty(name = "thluxury.payment.stripe.enabled", havingValue = "true")
public class StripeGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripeGateway.class);

    private final String secretKey;
    private final String defaultTestPaymentMethod;

    public StripeGateway(@Value("${thluxury.payment.stripe.secret-key:}") String secretKey,
                         @Value("${thluxury.payment.stripe.test-payment-method:pm_card_visa}") String testPm) {
        this.secretKey = secretKey;
        this.defaultTestPaymentMethod = testPm;
        log.info("StripeGateway enabled (key present: {})", secretKey != null && !secretKey.isBlank());
    }

    @Override
    public String name() { return "STRIPE"; }

    @Override
    public boolean supports(Payment.Method method) {
        return method == Payment.Method.CREDIT_CARD;
    }

    @Override
    public ChargeResult charge(ChargeCommand cmd) {
        String paymentMethod = (cmd.clientToken() != null && !cmd.clientToken().isBlank())
                ? cmd.clientToken() : defaultTestPaymentMethod;
        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(cmd.amount().longValueExact())
                    .setCurrency(cmd.currency() == null ? "vnd" : cmd.currency().toLowerCase())
                    .setConfirm(true)
                    .setPaymentMethod(paymentMethod)
                    .setDescription("THLuxury order " + cmd.orderId())
                    .putMetadata("orderId", cmd.orderId().toString())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .setAllowRedirects(
                                            PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                                    .build())
                    .build();

            RequestOptions opts = RequestOptions.builder()
                    .setApiKey(secretKey)
                    .setIdempotencyKey(cmd.idempotencyKey())
                    .build();

            PaymentIntent pi = PaymentIntent.create(params, opts);
            Payment.Status status = mapStatus(pi.getStatus());
            log.info("Stripe PaymentIntent {} → status {} (order {})", pi.getId(), pi.getStatus(), cmd.orderId());

            if (status == Payment.Status.FAILED) {
                return new ChargeResult(status, pi.getId(), "STRIPE_DECLINED",
                        "PaymentIntent status: " + pi.getStatus());
            }
            return new ChargeResult(status, pi.getId(), null, null);

        } catch (StripeException e) {
            log.error("Stripe charge failed for order {}: {}", cmd.orderId(), e.getMessage());
            return new ChargeResult(Payment.Status.FAILED, null,
                    e.getCode() == null ? "STRIPE_ERROR" : e.getCode(), e.getMessage());
        }
    }

    @Override
    public RefundResult refund(RefundCommand cmd) {
        try {
            RefundCreateParams.Builder b = RefundCreateParams.builder()
                    .setPaymentIntent(cmd.gatewayRef());
            if (cmd.amount() != null) b.setAmount(cmd.amount().longValueExact());
            Refund refund = Refund.create(b.build(),
                    RequestOptions.builder().setApiKey(secretKey).build());
            log.info("Stripe refund {} for PI {}", refund.getId(), cmd.gatewayRef());
            return new RefundResult(true, Payment.Status.REFUNDED, null, null);
        } catch (StripeException e) {
            log.error("Stripe refund failed for {}: {}", cmd.gatewayRef(), e.getMessage());
            return new RefundResult(false, Payment.Status.SUCCESS,
                    e.getCode() == null ? "STRIPE_REFUND_ERROR" : e.getCode(), e.getMessage());
        }
    }

    /** Map trạng thái PaymentIntent của Stripe → Payment.Status nội bộ. */
    static Payment.Status mapStatus(String stripeStatus) {
        if (stripeStatus == null) return Payment.Status.PENDING;
        return switch (stripeStatus) {
            case "succeeded" -> Payment.Status.SUCCESS;
            case "processing", "requires_action", "requires_confirmation", "requires_capture"
                    -> Payment.Status.PENDING;
            case "requires_payment_method", "canceled" -> Payment.Status.FAILED;
            default -> Payment.Status.PENDING;
        };
    }
}
