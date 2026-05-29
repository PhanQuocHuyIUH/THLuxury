package com.thluxury.payment.gateway;

import com.thluxury.payment.domain.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/** Quyết định gateway dùng cho mỗi request charge. */
@Component
public class PaymentGatewayResolver {

    private final List<PaymentGateway> gateways;
    private final boolean stripeEnabled;

    public PaymentGatewayResolver(List<PaymentGateway> gateways,
                                  @Value("${thluxury.payment.stripe.enabled:false}") boolean stripeEnabled) {
        this.gateways = gateways;
        this.stripeEnabled = stripeEnabled;
    }

    public PaymentGateway resolve(Payment.Method method) {
        if (stripeEnabled && method == Payment.Method.CREDIT_CARD) {
            return gateways.stream().filter(g -> "STRIPE".equals(g.name())).findFirst()
                    .orElseGet(this::mock);
        }
        return mock();
    }

    private PaymentGateway mock() {
        return gateways.stream().filter(g -> "MOCK".equals(g.name())).findFirst()
                .orElseThrow(() -> new IllegalStateException("MockGateway not configured"));
    }
}
