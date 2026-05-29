package com.thluxury.inventory;

import com.thluxury.inventory.domain.Inventory;
import com.thluxury.inventory.repository.InventoryMovementRepository;
import com.thluxury.inventory.repository.InventoryRepository;
import com.thluxury.inventory.service.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:tc:postgresql:16-alpine:///thluxury",
        "spring.datasource.username=test",
        "spring.datasource.password=test",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers
@ActiveProfiles("test")
public class InventoryConcurrencyTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryMovementRepository movementRepository;

    private UUID productId;
    private UUID branchId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        inventoryService.addStock(productId, branchId, 10, "INIT");
    }

    @AfterEach
    void tearDown() {
        movementRepository.deleteAll();
        inventoryRepository.deleteAll();
    }

    @Test
    void testConcurrentReservation() throws InterruptedException {
        int numThreads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    // Each thread tries to reserve 6 items. Total 10 items in stock.
                    // If both succeed, it would be 12 items reserved, which exceeds stock and/or breaks optimistic lock.
                    inventoryService.reserveStock(productId, branchId, 6, UUID.randomUUID().toString(), true);
                    successCount.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException | IllegalStateException e) {
                    failCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                    failCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        latch.countDown(); // Start all threads at the same time
        done.await();

        // 1 thread should succeed, 1 should fail (either optimistic lock or insufficient stock)
        assertEquals(1, successCount.get());
        assertEquals(1, failCount.get());

        Inventory inv = inventoryRepository.findByProductIdAndBranchId(productId, branchId).orElseThrow();
        assertEquals(6, inv.getReservedQuantity());
        assertEquals(10, inv.getQuantity());
    }
}
