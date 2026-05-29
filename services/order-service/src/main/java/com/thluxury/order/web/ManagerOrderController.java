package com.thluxury.order.web;

import com.thluxury.order.domain.OrderEntity;
import com.thluxury.order.domain.OrderStateMachine;
import com.thluxury.order.domain.OrderStatus;
import com.thluxury.order.repository.OrderRepository;
import com.thluxury.order.service.OrderStatusService;
import com.thluxury.order.web.dto.CheckoutDtos.OrderSummary;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders/manage")
public class ManagerOrderController {

    private final OrderRepository orders;
    private final OrderStatusService statusService;

    public ManagerOrderController(OrderRepository orders, OrderStatusService statusService) {
        this.orders = orders;
        this.statusService = statusService;
    }

    public record StatusUpdateRequest(@NotNull OrderStatus targetStatus) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public Map<String, Object> list(@RequestParam(required = false) List<OrderStatus> statuses,
                                    @RequestParam(required = false) UUID branchId,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        String role = jwt.getClaimAsString("role");
        String userBranch = jwt.getClaimAsString("branchId");

        UUID effectiveBranch = branchId;
        if ("BRANCH_MANAGER".equals(role)) {
            if (userBranch == null || userBranch.isEmpty()) {
                throw new IllegalArgumentException("BRANCH_MANAGER must have a branchId assigned");
            }
            effectiveBranch = UUID.fromString(userBranch);
        }

        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderEntity> p;
        if (effectiveBranch != null && statuses != null && !statuses.isEmpty()) {
            p = orders.findByBranchIdAndCurrentStatusIn(effectiveBranch, statuses, pr);
        } else if (effectiveBranch != null) {
            p = orders.findByBranchId(effectiveBranch, pr);
        } else if (statuses != null && !statuses.isEmpty()) {
            p = orders.findByCurrentStatusIn(statuses, pr);
        } else {
            p = orders.findAll(pr);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", p.getContent().stream().map(this::toRow).toList());
        out.put("page", p.getNumber());
        out.put("size", p.getSize());
        out.put("total", p.getTotalElements());
        return out;
    }

    @PatchMapping("/{maDh}/status")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public Map<String, Object> updateStatus(@PathVariable String maDh,
                                            @RequestBody @jakarta.validation.Valid StatusUpdateRequest req,
                                            Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        String role = jwt.getClaimAsString("role");
        String userBranch = jwt.getClaimAsString("branchId");
        OrderEntity o = statusService.transition(maDh, req.targetStatus(), role, userBranch);
        return toRow(o);
    }

    @GetMapping("/{maDh}")
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public Map<String, Object> getOrderDetail(@PathVariable String maDh, Authentication auth) {
        OrderEntity o = orders.findByMaDh(maDh).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Order not found"));
        Jwt jwt = (Jwt) auth.getPrincipal();
        String role = jwt.getClaimAsString("role");
        String userBranch = jwt.getClaimAsString("branchId");

        if ("BRANCH_MANAGER".equals(role)) {
            if (userBranch == null || o.getBranchId() == null || !userBranch.equals(o.getBranchId().toString())) {
                throw new org.springframework.security.access.AccessDeniedException("Not your branch's order");
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", o.getId());
        out.put("maDh", o.getMaDh());
        out.put("currentStatus", o.getCurrentStatus().name());
        out.put("deliveryType", o.getDeliveryType() == null ? null : o.getDeliveryType().name());
        out.put("branchId", o.getBranchId());
        out.put("subtotal", o.getSubtotal());
        out.put("discountAmount", o.getDiscountAmount());
        out.put("vatAmount", o.getVatAmount());
        out.put("vatPercent", o.getVatPercent());
        out.put("total", o.getTotal());
        out.put("voucherCode", o.getVoucherCode());
        out.put("paymentMethod", o.getPaymentMethod());
        out.put("failureReason", o.getFailureReason());
        out.put("customerSnapshot", o.getCustomerSnapshot());
        out.put("addressSnapshot", o.getAddressSnapshot());
        out.put("itemsSnapshot", o.getItemsSnapshot());
        out.put("createdAt", o.getCreatedAt());
        out.put("updatedAt", o.getUpdatedAt());
        out.put("nextStatuses", OrderStateMachine.nextStates(
                o.getCurrentStatus(), o.getDeliveryType()).stream().map(Enum::name).toList());
        return out;
    }

    /** Row cho admin: OrderSummary + danh sách trạng thái kế tiếp hợp lệ (để render nút). */
    private Map<String, Object> toRow(OrderEntity o) {
        OrderSummary s = new OrderSummary(
                o.getId(), o.getMaDh(), o.getCurrentStatus().name(),
                o.getSubtotal(), o.getDiscountAmount(), o.getVatAmount(), o.getTotal(),
                o.getVoucherCode(),
                o.getDeliveryType() == null ? null : o.getDeliveryType().name(),
                o.getBranchId(), o.getCreatedAt());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", s.id());
        row.put("maDh", s.maDh());
        row.put("currentStatus", s.currentStatus());
        row.put("subtotal", s.subtotal());
        row.put("discountAmount", s.discountAmount());
        row.put("vatAmount", s.vatAmount());
        row.put("total", s.total());
        row.put("voucherCode", s.voucherCode());
        row.put("deliveryType", s.deliveryType());
        row.put("branchId", s.branchId());
        row.put("createdAt", s.createdAt());
        row.put("nextStatuses", OrderStateMachine.nextStates(
                o.getCurrentStatus(), o.getDeliveryType()).stream().map(Enum::name).toList());
        return row;
    }
}
