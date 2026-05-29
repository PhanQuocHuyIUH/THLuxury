package com.thluxury.payment.messaging;

public final class PaymentTopology {
    private PaymentTopology() {}

    public static final String EXCHANGE = "payment.events";
    public static final String RK_PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String RK_PAYMENT_FAILED    = "payment.failed";
    public static final String RK_PAYMENT_REFUNDED  = "payment.refunded";
}
