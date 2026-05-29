package com.thluxury.inventory.messaging;

import com.thluxury.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Saga side-effect: sau khi PAYMENT_SUCCESS, Order publish `order.paid` lên
 * exchange `order.events`. Inventory consume để commit reserved → giảm quantity.
 *
 * Plan §4.4: "Listener Rabbit: order.paid.q → tự commit"
 */
@Component
public class OrderPaidConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderPaidConsumer.class);

    private final InventoryService inventory;

    public OrderPaidConsumer(InventoryService inventory) {
        this.inventory = inventory;
    }

    @RabbitListener(queues = InventoryTopology.ORDER_PAID_QUEUE)
    public void onOrderPaid(Map<String, Object> event) {
        Object orderIdObj = event.get("orderId");
        if (orderIdObj == null) {
            log.warn("order.paid received without orderId — payload={}", event);
            return;
        }
        String orderId = orderIdObj.toString();
        log.info("order.paid received → committing stock for order {}", orderId);
        try {
            inventory.commitStock(orderId);
        } catch (Exception e) {
            // Để RabbitMQ retry → DLQ nếu vẫn fail.
            log.error("Failed to commit stock for order {}", orderId, e);
            throw new RuntimeException(e);
        }
    }
}
