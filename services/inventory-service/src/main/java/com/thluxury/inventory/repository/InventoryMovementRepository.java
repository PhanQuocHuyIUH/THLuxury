package com.thluxury.inventory.repository;

import com.thluxury.inventory.domain.InvMovType;
import com.thluxury.inventory.domain.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, UUID> {
    List<InventoryMovement> findByProductIdAndBranchIdOrderByCreatedAtDesc(UUID productId, UUID branchId);
    List<InventoryMovement> findByReferenceAndType(String reference, InvMovType type);
}
