package com.thluxury.catalog.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sau commit của lệnh ghi (create/update/delete), publish event lên RabbitMQ.
 * Dùng @TransactionalEventListener(AFTER_COMMIT) thay cho outbox table — đơn giản
 * và đủ tốt cho demo. Trade-off: nếu broker down ngay sau commit DB thì event mất.
 */
@Component
public class ProductEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ProductEventPublisher.class);

    private final RabbitTemplate rabbit;

    public ProductEventPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @EventListener
    public void onProductChanged(ProductChangedEvent event) {
        String rk = switch (event.type()) {
            case "PRODUCT_CREATED" -> CatalogTopology.RK_PRODUCT_CREATED;
            case "PRODUCT_UPDATED" -> CatalogTopology.RK_PRODUCT_UPDATED;
            case "PRODUCT_DELETED" -> CatalogTopology.RK_PRODUCT_DELETED;
            default -> CatalogTopology.RK_PRODUCT_UPDATED;
        };
        rabbit.convertAndSend(CatalogTopology.EXCHANGE, rk, event);
        log.info("Published {} for product {} (maSp={})", event.type(), event.productId(), event.maSp());
    }
}
