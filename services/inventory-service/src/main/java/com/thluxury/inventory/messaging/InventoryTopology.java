package com.thluxury.inventory.messaging;

/** Tên chuẩn exchange / queue / routing key Inventory đang dùng. */
public final class InventoryTopology {
    private InventoryTopology() {}

    public static final String ORDER_EXCHANGE = "order.events";

    public static final String ORDER_PAID_QUEUE = "inventory.order-paid.q";
    public static final String ORDER_PAID_DLQ   = "inventory.order-paid.dlq";

    public static final String RK_ORDER_PAID     = "order.paid";
    public static final String RK_ORDER_CANCELLED = "order.cancelled";
}
