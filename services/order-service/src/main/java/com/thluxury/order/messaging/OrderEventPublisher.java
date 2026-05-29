package com.thluxury.order.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thluxury.order.domain.OrderEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publish lifecycle event lên `order.events`.
 *  - order.created  (sau khi Saga step 1 commit)
 *  - order.paid     (S5)
 *  - order.failed   (compensate)
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbit;
    private final ObjectMapper json;

    public OrderEventPublisher(RabbitTemplate rabbit, ObjectMapper json) {
        this.rabbit = rabbit;
        this.json = json;
    }

    public void publishCreated(OrderEntity order) {
        publish(OrderTopology.RK_ORDER_CREATED, basePayload(order));
    }

    public void publishFailed(OrderEntity order, String reason) {
        Map<String, Object> payload = basePayload(order);
        payload.put("reason", reason);
        publish(OrderTopology.RK_ORDER_FAILED, payload);
    }

    public void publishPaid(OrderEntity order) {
        publish(OrderTopology.RK_ORDER_PAID, basePayload(order));
    }

    public void publishStatusChanged(OrderEntity order, String fromStatus) {
        Map<String, Object> payload = basePayload(order);
        payload.put("fromStatus", fromStatus);
        payload.put("toStatus", order.getCurrentStatus().name());
        publish(OrderTopology.RK_ORDER_STATUS, payload);
    }

    private Map<String, Object> basePayload(OrderEntity order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", order.getId().toString());
        m.put("maDh", order.getMaDh());
        m.put("customerId", order.getCustomerId().toString());
        m.put("branchId", order.getBranchId().toString());
        m.put("status", order.getCurrentStatus().name());
        m.put("total", order.getTotal());

        String email = null;
        String fullName = null;
        String snapshot = order.getCustomerSnapshot();
        if (snapshot != null && !snapshot.isBlank()) {
            try {
                JsonNode node = json.readTree(snapshot);
                if (node.hasNonNull("email")) email = node.get("email").asText();
                if (node.hasNonNull("fullName")) fullName = node.get("fullName").asText();
            } catch (Exception e) {
                log.warn("Cannot parse customer_snapshot for {}: {}", order.getMaDh(), e.getMessage());
            }
        }
        m.put("email", email);
        m.put("fullName", fullName);
        return m;
    }

    private void publish(String routingKey, Map<String, Object> payload) {
        try {
            rabbit.convertAndSend(OrderTopology.EXCHANGE, routingKey, payload);
            log.debug("Published {} → {}", routingKey, payload.get("maDh"));
        } catch (Exception e) {
            log.error("Failed to publish {} for {}", routingKey, payload.get("maDh"), e);
        }
    }
}
