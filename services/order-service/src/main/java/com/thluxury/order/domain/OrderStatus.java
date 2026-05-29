package com.thluxury.order.domain;

public enum OrderStatus {
    CREATED, RESERVED, PRICED,
    PAID,        // thanh toán online đã capture
    CONFIRMED,   // COD: đơn đã chốt, CHƯA thu tiền (thu khi giao/nhận)
    PREPARING,
    READY_FOR_PICKUP, SHIPPING, DELIVERED, COMPLETED,
    CANCELLED, FAILED
}
