package com.thluxury.catalog.messaging;

import com.thluxury.catalog.service.ProductCache;
import com.thluxury.catalog.service.ProductViewProjector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Consume catalog.events từ queue catalog.projection.q:
 *   1. Cập nhật read model (product_view).
 *   2. Invalidate Redis cache (detail + tất cả list).
 */
@Component
public class ProductEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ProductEventConsumer.class);

    private final ProductViewProjector projector;
    private final ProductCache cache;

    public ProductEventConsumer(ProductViewProjector projector, ProductCache cache) {
        this.projector = projector;
        this.cache = cache;
    }

    @RabbitListener(queues = CatalogTopology.PROJECTION_QUEUE)
    public void onEvent(ProductChangedEvent event) {
        log.info("Consumed {} for product {} (maSp={})", event.type(), event.productId(), event.maSp());
        try {
            if ("PRODUCT_DELETED".equals(event.type())) {
                projector.delete(event.productId());
            } else {
                projector.rebuild(event.productId());
            }
            cache.invalidateDetail(event.productId());
            cache.invalidateAllLists();
        } catch (Exception e) {
            // Throw lại để Rabbit retry / DLQ
            log.error("Projection failed for {}: {}", event.productId(), e.getMessage(), e);
            throw e;
        }
    }
}
