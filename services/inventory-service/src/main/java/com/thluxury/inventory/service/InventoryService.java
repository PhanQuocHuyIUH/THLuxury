package com.thluxury.inventory.service;

import com.thluxury.inventory.domain.InvMovType;
import com.thluxury.inventory.domain.Inventory;
import com.thluxury.inventory.domain.InventoryMovement;
import com.thluxury.inventory.repository.InventoryMovementRepository;
import com.thluxury.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepo;
    private final InventoryMovementRepository movementRepo;

    public InventoryService(InventoryRepository inventoryRepo, InventoryMovementRepository movementRepo) {
        this.inventoryRepo = inventoryRepo;
        this.movementRepo = movementRepo;
    }

    @Transactional
    public void addStock(UUID productId, UUID branchId, int quantity, String reference, UUID createdBy) {
        Inventory inv = inventoryRepo.findByProductIdAndBranchId(productId, branchId)
                .orElseGet(() -> {
                    Inventory newInv = new Inventory();
                    newInv.setProductId(productId);
                    newInv.setBranchId(branchId);
                    return newInv;
                });
        inv.setQuantity(inv.getQuantity() + quantity);
        inventoryRepo.save(inv);

        InventoryMovement mov = new InventoryMovement(productId, branchId, InvMovType.STOCK_IN, quantity, reference);
        mov.setCreatedBy(createdBy);
        movementRepo.save(mov);
    }

    /** Backward-compatible overload (test/seeder dùng). */
    @Transactional
    public void addStock(UUID productId, UUID branchId, int quantity, String reference) {
        addStock(productId, branchId, quantity, reference, null);
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public int computeAvailable(UUID productId, UUID branchId) {
        if (branchId != null) {
            return inventoryRepo.findByProductIdAndBranchId(productId, branchId)
                    .map(inv -> inv.getQuantity() - inv.getReservedQuantity())
                    .orElse(0);
        }
        // Tổng available trên mọi branch
        return inventoryRepo.findByProductId(productId).stream()
                .mapToInt(inv -> inv.getQuantity() - inv.getReservedQuantity())
                .sum();
    }

    @Transactional
    public String reserveStock(UUID productId, UUID branchId, int quantity, String orderIdStr, boolean strictBranch) {
        if (branchId != null) {
            return doReserve(productId, branchId, quantity, orderIdStr);
        }
        if (strictBranch) {
            throw new IllegalArgumentException("Branch ID is required when strict_branch=true");
        }
        // Auto-pick branch còn hàng (plan §4.4: policy HCM→HN→ĐN — ở đây sort theo available desc).
        List<Inventory> candidates = inventoryRepo.findByProductId(productId).stream()
                .filter(inv -> inv.getQuantity() - inv.getReservedQuantity() >= quantity)
                .sorted((a, b) -> Integer.compare(
                        b.getQuantity() - b.getReservedQuantity(),
                        a.getQuantity() - a.getReservedQuantity()))
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("INSUFFICIENT_STOCK");
        }
        return doReserve(productId, candidates.get(0).getBranchId(), quantity, orderIdStr);
    }

    private String doReserve(UUID productId, UUID branchId, int quantity, String orderIdStr) {
        Inventory inv = inventoryRepo.findByProductIdAndBranchId(productId, branchId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found in inventory"));
        
        try {
            inv.reserve(quantity);
            inventoryRepo.save(inv);
            movementRepo.save(new InventoryMovement(productId, branchId, InvMovType.RESERVE, quantity, orderIdStr));
            return branchId.toString();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("INSUFFICIENT_STOCK");
        }
    }

    @Transactional
    public void commitStock(String orderIdStr) {
        log.info("Committing stock for order {}", orderIdStr);
        List<InventoryMovement> reserves = movementRepo.findByReferenceAndType(orderIdStr, InvMovType.RESERVE);
        for (InventoryMovement mov : reserves) {
            Inventory inv = inventoryRepo.findByProductIdAndBranchId(mov.getProductId(), mov.getBranchId())
                    .orElseThrow();
            inv.commit(mov.getQuantity());
            inventoryRepo.save(inv);
            movementRepo.save(new InventoryMovement(mov.getProductId(), mov.getBranchId(), InvMovType.COMMIT, mov.getQuantity(), orderIdStr));
        }
    }

    @Transactional
    public void releaseStock(String orderIdStr) {
        log.info("Releasing stock for order {}", orderIdStr);
        List<InventoryMovement> reserves = movementRepo.findByReferenceAndType(orderIdStr, InvMovType.RESERVE);
        for (InventoryMovement mov : reserves) {
            Inventory inv = inventoryRepo.findByProductIdAndBranchId(mov.getProductId(), mov.getBranchId())
                    .orElseThrow();
            inv.release(mov.getQuantity());
            inventoryRepo.save(inv);
            movementRepo.save(new InventoryMovement(mov.getProductId(), mov.getBranchId(), InvMovType.RELEASE, mov.getQuantity(), orderIdStr));
        }
    }
}
