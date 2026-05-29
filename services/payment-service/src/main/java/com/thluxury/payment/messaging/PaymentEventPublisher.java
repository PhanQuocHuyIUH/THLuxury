package com.thluxury.payment.messaging;

import com.thluxury.payment.domain.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class PaymentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisher.class);
    private final RabbitTemplate rabbit;

    public PaymentEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    public void publishSucceeded(Payment p) {
        publish(PaymentTopology.RK_PAYMENT_SUCCEEDED, p, null);
    }

    public void publishFailed(Payment p, String errorCode) {
        publish(PaymentTopology.RK_PAYMENT_FAILED, p, errorCode);
    }

    public void publishRefunded(Payment p) {
        publish(PaymentTopology.RK_PAYMENT_REFUNDED, p, null);
    }

    private void publish(String rk, Payment p, String errorCode) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("paymentId", p.getId().toString());
        m.put("orderId", p.getOrderId().toString());
        m.put("status", p.getStatus().name());
        m.put("method", p.getMethod().name());
        m.put("amount", p.getAmount());
        m.put("provider", p.getProvider().name());
        if (errorCode != null) m.put("errorCode", errorCode);
        try {
            rabbit.convertAndSend(PaymentTopology.EXCHANGE, rk, m);
            log.debug("Published {} for payment {}", rk, p.getId());
        } catch (Exception e) {
            log.error("Failed to publish {} for {}", rk, p.getId(), e);
        }
    }
}
