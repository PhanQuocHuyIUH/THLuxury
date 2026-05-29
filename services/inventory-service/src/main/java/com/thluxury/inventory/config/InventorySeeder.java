package com.thluxury.inventory.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.thluxury.inventory.repository.InventoryRepository;
import com.thluxury.inventory.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Seed initial stock cho 3 branch × N sản phẩm (plan §6.5: random 5–30/cặp).
 * Cách lấy ID:
 *   - Branches: GET {identity}/api/branches (public)
 *   - Products: GET {catalog}/api/products?size=500 (public)
 * Có retry vì catalog/identity có thể chưa healthy ngay.
 * Idempotent: nếu inventory đã có row → skip.
 */
@Component
public class InventorySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InventorySeeder.class);

    private final InventoryRepository inventoryRepo;
    private final InventoryService inventoryService;
    private final WebClient catalogClient;
    private final WebClient identityClient;

    private final boolean enabled;
    private final int min;
    private final int max;

    public InventorySeeder(InventoryRepository inventoryRepo,
                           InventoryService inventoryService,
                           @Value("${thluxury.inventory.catalog-base-url}") String catalogUrl,
                           @Value("${thluxury.inventory.identity-base-url}") String identityUrl,
                           @Value("${thluxury.inventory.seed.enabled:true}") boolean enabled,
                           @Value("${thluxury.inventory.seed.min:5}") int min,
                           @Value("${thluxury.inventory.seed.max:30}") int max) {
        this.inventoryRepo = inventoryRepo;
        this.inventoryService = inventoryService;
        this.catalogClient = WebClient.builder().baseUrl(catalogUrl).build();
        this.identityClient = WebClient.builder().baseUrl(identityUrl).build();
        this.enabled = enabled;
        this.min = min;
        this.max = max;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("InventorySeeder: disabled");
            return;
        }
        if (inventoryRepo.count() > 0) {
            log.info("InventorySeeder: skipped — inventory table not empty");
            return;
        }

        try {
            List<UUID> branches = fetchBranchIds();
            List<UUID> products = fetchProductIds();
            if (branches.isEmpty() || products.isEmpty()) {
                log.warn("InventorySeeder: no branches ({}) or products ({}) found — skip", branches.size(), products.size());
                return;
            }
            int rows = 0;
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (UUID productId : products) {
                for (UUID branchId : branches) {
                    int qty = rnd.nextInt(min, max + 1);
                    inventoryService.addStock(productId, branchId, qty, "SEED", null);
                    rows++;
                }
            }
            log.info("InventorySeeder: seeded {} inventory rows ({} products × {} branches, qty {}–{})",
                    rows, products.size(), branches.size(), min, max);
        } catch (Exception e) {
            log.error("InventorySeeder failed — startup continues", e);
        }
    }

    private List<UUID> fetchBranchIds() {
        JsonNode arr = identityClient.get().uri("/api/branches")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .onErrorResume(e -> { log.warn("Cannot fetch branches: {}", e.toString()); return Mono.empty(); })
                .block(Duration.ofSeconds(60));
        List<UUID> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) {
                JsonNode id = n.get("id");
                if (id != null && !id.isNull()) {
                    out.add(UUID.fromString(id.asText()));
                }
            }
        }
        return out;
    }

    private List<UUID> fetchProductIds() {
        JsonNode page = catalogClient.get().uri("/api/products?size=500")
                .retrieve()
                .bodyToMono(JsonNode.class)
                .retryWhen(Retry.backoff(5, Duration.ofSeconds(2)).maxBackoff(Duration.ofSeconds(10)))
                .onErrorResume(e -> { log.warn("Cannot fetch products: {}", e.toString()); return Mono.empty(); })
                .block(Duration.ofSeconds(60));
        List<UUID> out = new ArrayList<>();
        if (page != null && page.has("content") && page.get("content").isArray()) {
            for (JsonNode n : page.get("content")) {
                JsonNode id = n.get("id");
                if (id != null && !id.isNull()) {
                    out.add(UUID.fromString(id.asText()));
                }
            }
        }
        return out;
    }
}
