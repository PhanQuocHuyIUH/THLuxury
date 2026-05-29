package com.thluxury.notification.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.thluxury.notification.config.RabbitMQConfig;
import com.thluxury.notification.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final MailService mailService;

    @Value("${thluxury.frontend-url}")
    private String frontendUrl;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CONFIRMED)
    public void handleOrderConfirmed(JsonNode payload) {
        log.info("Received order.paid event: {}", payload);
        try {
            String email = getEmailFromOrderPayload(payload);
            String maDh = payload.path("ma_dh").asText(payload.path("maDh").asText(""));
            String fullName = payload.path("customer_snapshot").path("fullName").asText("Quý khách");

            Map<String, Object> variables = new HashMap<>();
            variables.put("fullName", fullName);
            variables.put("maDh", maDh);
            variables.put("total", payload.path("total").asText("0"));
            variables.put("orderUrl", frontendUrl + "/order/" + maDh);
            // Có thể thêm chi tiết items nếu cần

            mailService.sendEmail(
                    email,
                    "Đặt hàng thành công - Đơn hàng " + maDh,
                    "order-confirmed",
                    variables,
                    "ORDER_CONFIRMED"
            );
        } catch (Exception e) {
            log.error("Error processing order confirmed event", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_STATUS)
    public void handleOrderStatusChanged(JsonNode payload) {
        log.info("Received order.status.changed event: {}", payload);
        try {
            String email = getEmailFromOrderPayload(payload);
            String maDh = payload.path("ma_dh").asText(payload.path("maDh").asText(""));
            String status = payload.path("current_status").asText(payload.path("status").asText(""));
            String fullName = payload.path("customer_snapshot").path("fullName").asText("Quý khách");

            Map<String, Object> variables = new HashMap<>();
            variables.put("fullName", fullName);
            variables.put("maDh", maDh);
            variables.put("status", status);
            variables.put("orderUrl", frontendUrl + "/order/" + maDh);

            mailService.sendEmail(
                    email,
                    "Cập nhật trạng thái đơn hàng " + maDh,
                    "order-status",
                    variables,
                    "ORDER_STATUS"
            );
        } catch (Exception e) {
            log.error("Error processing order status event", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_FAILED)
    public void handleOrderFailed(JsonNode payload) {
        log.info("Received order.failed event: {}", payload);
        try {
            String email = getEmailFromOrderPayload(payload);
            String maDh = payload.path("ma_dh").asText(payload.path("maDh").asText(""));
            String reason = payload.path("failure_reason").asText(payload.path("reason").asText("Lỗi thanh toán"));
            String fullName = payload.path("customer_snapshot").path("fullName").asText("Quý khách");

            Map<String, Object> variables = new HashMap<>();
            variables.put("fullName", fullName);
            variables.put("maDh", maDh);
            variables.put("reason", reason);

            mailService.sendEmail(
                    email,
                    "Đơn hàng " + maDh + " đã bị hủy do lỗi",
                    "order-failed",
                    variables,
                    "ORDER_FAILED"
            );
        } catch (Exception e) {
            log.error("Error processing order failed event", e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_AUTH_RESET)
    public void handleAuthPasswordReset(JsonNode payload) {
        log.info("Received auth.password.reset event: {}", payload);
        try {
            String email = payload.path("email").asText();
            String token = payload.path("token").asText();

            Map<String, Object> variables = new HashMap<>();
            variables.put("resetUrl", frontendUrl + "/reset-password?token=" + token);

            mailService.sendEmail(
                    email,
                    "Yêu cầu đặt lại mật khẩu",
                    "auth-reset",
                    variables,
                    "AUTH_RESET"
            );
        } catch (Exception e) {
            log.error("Error processing auth reset event", e);
        }
    }

    private String getEmailFromOrderPayload(JsonNode payload) {
        // Có thể nằm ở customer_snapshot.email
        JsonNode customerSnapshot = payload.path("customer_snapshot");
        if (!customerSnapshot.isMissingNode() && customerSnapshot.has("email")) {
            return customerSnapshot.path("email").asText();
        }
        return payload.path("email").asText("test@example.com"); // Fallback
    }
}
