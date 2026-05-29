package com.thluxury.order.messaging;

public final class OrderTopology {
    private OrderTopology() {}

    public static final String EXCHANGE = "order.events";

    public static final String RK_ORDER_CREATED = "order.created";
    public static final String RK_ORDER_PAID    = "order.paid";
    public static final String RK_ORDER_FAILED  = "order.failed";
    public static final String RK_ORDER_STATUS  = "order.status.changed";
}
