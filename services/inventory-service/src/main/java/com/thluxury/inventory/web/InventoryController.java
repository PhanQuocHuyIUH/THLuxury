package com.thluxury.inventory.web;

import com.thluxury.inventory.domain.Inventory;
import com.thluxury.inventory.repository.InventoryMovementRepository;
import com.thluxury.inventory.repository.InventoryRepository;
import com.thluxury.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final InventoryService inventoryService;

    @Value("${thluxury.inventory.low-stock-threshold-default:5}")
    private int defaultLowStockThreshold;

    public InventoryController(InventoryRepository inventoryRepository,
                               InventoryMovementRepository movementRepository,
                               InventoryService inventoryService) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.inventoryService = inventoryService;
    }

    private UUID userIdFrom(Authentication auth) {
        try {
            return UUID.fromString(((Jwt) auth.getPrincipal()).getSubject());
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public List<InventoryDto> listInventory(
            @RequestParam(required = false) UUID branchId,
            Authentication auth) {
        
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userRole = jwt.getClaimAsString("role");
        String userBranch = jwt.getClaimAsString("branchId");
        
        UUID effectiveBranchId = branchId;
        if ("BRANCH_MANAGER".equals(userRole)) {
            if (userBranch != null && !userBranch.isEmpty()) {
                effectiveBranchId = UUID.fromString(userBranch);
            } else {
                throw new IllegalArgumentException("BRANCH_MANAGER must have a branchId assigned");
            }
        }

        List<Inventory> list;
        if (effectiveBranchId != null) {
            list = inventoryRepository.findByBranchId(effectiveBranchId);
        } else {
            list = inventoryRepository.findAll();
        }
        
        return list.stream().map(inv -> new InventoryDto(
                inv.getId(),
                inv.getProductId(),
                inv.getBranchId(),
                inv.getQuantity(),
                inv.getReservedQuantity()
        )).collect(Collectors.toList());
    }

    @PostMapping("/stock-in")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public ResponseEntity<Void> stockIn(@RequestBody StockInRequest req, Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userRole = jwt.getClaimAsString("role");
        String userBranch = jwt.getClaimAsString("branchId");
        
        UUID effectiveBranchId = req.branchId();
        if ("BRANCH_MANAGER".equals(userRole)) {
            if (userBranch != null && !userBranch.isEmpty()) {
                effectiveBranchId = UUID.fromString(userBranch);
            }
        }
        
        if (effectiveBranchId == null) {
            throw new IllegalArgumentException("Branch ID is required");
        }

        inventoryService.addStock(req.productId(), effectiveBranchId, req.quantity(), req.reference(), userIdFrom(auth));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public List<InventoryDto> lowStock(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) Integer threshold,
            Authentication auth) {

        Jwt jwt = (Jwt) auth.getPrincipal();
        String userRole = jwt.getClaimAsString("role");
        String userBranch = jwt.getClaimAsString("branchId");

        UUID effectiveBranchId = branchId;
        if ("BRANCH_MANAGER".equals(userRole)) {
            if (userBranch != null && !userBranch.isEmpty()) {
                effectiveBranchId = UUID.fromString(userBranch);
            } else {
                throw new IllegalArgumentException("BRANCH_MANAGER must have a branchId assigned");
            }
        }
        int t = threshold == null ? defaultLowStockThreshold : threshold;
        return inventoryRepository.findLowStock(effectiveBranchId, t).stream()
                .map(inv -> new InventoryDto(inv.getId(), inv.getProductId(), inv.getBranchId(),
                        inv.getQuantity(), inv.getReservedQuantity()))
                .collect(Collectors.toList());
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'BRANCH_MANAGER')")
    public List<InventoryMovementDto> getMovements(
            @RequestParam UUID productId,
            @RequestParam(required = false) UUID branchId,
            Authentication auth) {
        
        Jwt jwt = (Jwt) auth.getPrincipal();
        String userRole = jwt.getClaimAsString("role");
        String userBranch = jwt.getClaimAsString("branchId");
        
        UUID effectiveBranchId = branchId;
        if ("BRANCH_MANAGER".equals(userRole)) {
            if (userBranch != null && !userBranch.isEmpty()) {
                effectiveBranchId = UUID.fromString(userBranch);
            }
        }

        if (effectiveBranchId == null) {
            throw new IllegalArgumentException("Branch ID is required");
        }

        return movementRepository.findByProductIdAndBranchIdOrderByCreatedAtDesc(productId, effectiveBranchId)
                .stream().map(m -> new InventoryMovementDto(
                        m.getId(),
                        m.getProductId(),
                        m.getBranchId(),
                        m.getType().name(),
                        m.getQuantity(),
                        m.getReference(),
                        m.getCreatedAt().toString()
                )).collect(Collectors.toList());
    }

    public record InventoryDto(UUID id, UUID productId, UUID branchId, int quantity, int reservedQuantity) {}
    public record StockInRequest(UUID productId, UUID branchId, int quantity, String reference) {}
    public record InventoryMovementDto(UUID id, UUID productId, UUID branchId, String type, int quantity, String reference, String createdAt) {}
}
