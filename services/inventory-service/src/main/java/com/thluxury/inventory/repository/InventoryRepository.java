package com.thluxury.inventory.repository;

import com.thluxury.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findByProductIdAndBranchId(UUID productId, UUID branchId);
    List<Inventory> findByBranchId(UUID branchId);
    List<Inventory> findByProductId(UUID productId);

    @org.springframework.data.jpa.repository.Query("""
        SELECT i FROM Inventory i
        WHERE (:branchId IS NULL OR i.branchId = :branchId)
          AND (i.quantity - i.reservedQuantity) <= :threshold
        ORDER BY (i.quantity - i.reservedQuantity) ASC
    """)
    List<Inventory> findLowStock(@org.springframework.data.repository.query.Param("branchId") UUID branchId,
                                 @org.springframework.data.repository.query.Param("threshold") int threshold);
}
