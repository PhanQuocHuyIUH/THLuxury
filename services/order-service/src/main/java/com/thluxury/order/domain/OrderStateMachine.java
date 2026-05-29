package com.thluxury.order.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Vòng đời đơn hàng do manager/admin điều khiển (sau khi Saga đã đưa đơn về
 * PAID hoặc CONFIRMED). Các trạng thái CREATED/RESERVED/PRICED là bước nội bộ
 * của Saga, không cho manager can thiệp.
 *
 *  PAID/CONFIRMED → PREPARING → READY_FOR_PICKUP (pickup) → COMPLETED
 *                            → SHIPPING (delivery) → DELIVERED → COMPLETED
 *  (mọi trạng thái còn hoạt động) → CANCELLED
 */
public final class OrderStateMachine {
    private OrderStateMachine() {}

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);
    static {
        ALLOWED.put(OrderStatus.PAID,             EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.CONFIRMED,        EnumSet.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.PREPARING,        EnumSet.of(OrderStatus.READY_FOR_PICKUP, OrderStatus.SHIPPING, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.READY_FOR_PICKUP, EnumSet.of(OrderStatus.COMPLETED, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.SHIPPING,         EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED));
        ALLOWED.put(OrderStatus.DELIVERED,        EnumSet.of(OrderStatus.COMPLETED));
        // CREATED/RESERVED/PRICED/COMPLETED/CANCELLED/FAILED: không cho manager chuyển.
    }

    /** Trạng thái kế tiếp hợp lệ cho 1 đơn, đã lọc theo loại giao hàng. */
    public static Set<OrderStatus> nextStates(OrderStatus from, DeliveryType deliveryType) {
        Set<OrderStatus> base = ALLOWED.getOrDefault(from, EnumSet.noneOf(OrderStatus.class));
        Set<OrderStatus> out = EnumSet.noneOf(OrderStatus.class);
        for (OrderStatus to : base) {
            if (isDeliveryCompatible(to, deliveryType)) out.add(to);
        }
        return out;
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to, DeliveryType deliveryType) {
        return nextStates(from, deliveryType).contains(to);
    }

    private static boolean isDeliveryCompatible(OrderStatus to, DeliveryType deliveryType) {
        return switch (to) {
            case READY_FOR_PICKUP -> deliveryType == DeliveryType.STORE_PICKUP;
            case SHIPPING, DELIVERED -> deliveryType == DeliveryType.HOME_DELIVERY;
            default -> true;
        };
    }
}
