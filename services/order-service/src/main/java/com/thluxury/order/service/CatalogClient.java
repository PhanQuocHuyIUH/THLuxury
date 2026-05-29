package com.thluxury.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.UUID;

/** REST internal sang Catalog Service để pricing + snapshot item. */
@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

    private final WebClient client;

    public CatalogClient(@Value("${thluxury.order.catalog-base-url}") String baseUrl) {
        this.client = WebClient.builder().baseUrl(baseUrl).build();
    }

    public JsonNode getProduct(UUID productId) {
        return client.get().uri("/api/products/{id}", productId)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(Duration.ofSeconds(5));
    }
}
