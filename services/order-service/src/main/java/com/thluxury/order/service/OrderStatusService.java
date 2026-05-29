package com.thluxury.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thluxury.order.domain.OrderEntity;
import com.thluxury.order.domain.OrderEvent;
import com.thluxury.order.domain.OrderStateMachine;
import com.thluxury.order.domain.OrderStatus;
import com.thluxury.order.grpc.InventoryGrpcClient;
import com.thluxury.order.messaging.OrderEventPublisher;
import com.thluxury.order.repository.OrderEventRepository;
import com.thluxury.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Chuyển trạng thái đơn do manager/admin thực hiện, có kiểm soát:
 *  - state machine (transition hợp lệ + tương thích loại giao hàng),
 *  - phân quyền chi nhánh (BRANCH_MANAGER chỉ thao tác đơn của chi nhánh mình),
 *  - bù trừ kho khi huỷ (release reserved),
 *  - ghi OrderEvent + publish order.status.changed (Notification).
 */
@Service
public class OrderStatusService {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusService.class);

    private final OrderRepository orders;
    private final OrderEventRepository events;
    private final OrderEventPublisher publisher;
    private final InventoryGrpcClient inventory;
    private final ObjectMapper json;

    public OrderStatusService(OrderRepository orders,
                              OrderEventRepository events,
                              OrderEventPublisher publisher,
                              InventoryGrpcClient inventory,
                              ObjectMapper json) {
        this.orders = orders;
        this.events = events;
        this.publisher = publisher;
        this.inventory = inventory;
        this.json = json;
    }

    @Transactional
    public OrderEntity transition(String maDh, OrderStatus target, String role, String userBranchId) {
        OrderEntity order = orders.findByMaDh(maDh)
                .orElseThrow(() -> new java.util.NoSuchElementException("ORDER_NOT_FOUND:" + maDh));

        // Phân quyền: BRANCH_MANAGER chỉ thao tác đơn thuộc chi nhánh được gán.
        if ("BRANCH_MANAGER".equals(role)) {
            if (userBranchId == null || userBranchId.isBlank()) {
                throw new IllegalArgumentException("BRANCH_MANAGER must have a branchId assigned");
            }
            if (!UUID.fromString(userBranchId).equals(order.getBranchId())) {
                throw new AccessDeniedException("Đơn không thuộc chi nhánh của bạn");
            }
        }

        OrderStatus from = order.getCurrentStatus();
        if (!OrderStateMachine.canTransition(from, target, order.getDeliveryType())) {
            throw new IllegalStateException("INVALID_TRANSITION:" + from + "->" + target);
        }

        // Huỷ đơn → giải phóng tồn kho đã giữ (compensating).
        if (target == OrderStatus.CANCELLED) {
            inventory.release(order.getId(), "ORDER_CANCELLED_BY_MANAGER");
        }

        order.setCurrentStatus(target);
        orders.save(order);

        appendEvent(order.getId(), "STATUS_CHANGED", Map.of(
                "from", from.name(),
                "to", target.name()));
        publisher.publishStatusChanged(order, from.name());
        log.info("Order {} status {} → {}", maDh, from, target);
        return order;
    }

    private void appendEvent(UUID orderId, String type, Map<String, ?> data) {
        int seq = (int) (events.countByOrderId(orderId) + 1);
        try {
            events.save(new OrderEvent(orderId, seq, type, json.writeValueAsString(data)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
