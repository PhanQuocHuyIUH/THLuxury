package com.thluxury.catalog.messaging;

import java.time.Instant;
import java.util.UUID;

/**
 * Event payload publish lên exchange catalog.events khi sản phẩm bị tạo/sửa/xóa.
 * Read model (product_view) + AI Service consume event này.
 *
 * Sử dụng record để serialize Jackson JSON đơn giản.
 */
public record ProductChangedEvent(
        String type,           // PRODUCT_CREATED | PRODUCT_UPDATED | PRODUCT_DELETED
        UUID productId,
        Long maSp,
        Instant occurredAt,
        String triggeredBy     // userId của người gây ra
) {}
