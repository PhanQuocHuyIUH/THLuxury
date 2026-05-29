package com.thluxury.catalog.messaging;

/** Tên chuẩn cho exchange / queue / routing key của catalog domain. */
public final class CatalogTopology {
    private CatalogTopology() {}

    public static final String EXCHANGE = "catalog.events";

    // Queue mà chính Catalog Service consume để rebuild read model.
    public static final String PROJECTION_QUEUE = "catalog.projection.q";
    public static final String PROJECTION_DLQ   = "catalog.projection.dlq";

    // Routing keys
    public static final String RK_PRODUCT_CREATED = "product.created";
    public static final String RK_PRODUCT_UPDATED = "product.updated";
    public static final String RK_PRODUCT_DELETED = "product.deleted";
    public static final String RK_PRODUCT_ANY     = "product.*";
}
