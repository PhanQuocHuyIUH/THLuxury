package com.thluxury.order.web;

import com.thluxury.order.domain.OrderEntity;
import com.thluxury.order.domain.OrderEvent;
import com.thluxury.order.domain.OrderStatus;
import com.thluxury.order.repository.OrderEventRepository;
import com.thluxury.order.repository.OrderRepository;
import com.thluxury.order.saga.OrderSagaService;
import com.thluxury.order.web.dto.CheckoutDtos.*;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderSagaService saga;
    private final OrderRepository orders;
    private final OrderEventRepository events;

    public OrderController(OrderSagaService saga,
                           OrderRepository orders,
                           OrderEventRepository events) {
        this.saga = saga;
        this.orders = orders;
        this.events = events;
    }

    private UUID userId(Authentication auth) {
        return UUID.fromString(((Jwt) auth.getPrincipal()).getSubject());
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderSummary> checkout(@RequestBody @Valid CheckoutRequest req,
                                                 Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        CustomerInput customer = req.customer() != null ? req.customer()
                : new CustomerInput(jwt.getClaimAsString("fullName"),
                                    jwt.getClaimAsString("phone"),
                                    jwt.getClaimAsString("email"));
        OrderEntity order = saga.checkout(userId(auth), customer, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(toSummary(order));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Map<String, Object> myOrders(@RequestParam(required = false) OrderStatus status,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "10") int size,
                                        Authentication auth) {
        UUID uid = userId(auth);
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderEntity> p = status == null
                ? orders.findByCustomerId(uid, pr)
                : orders.findByCustomerIdAndCurrentStatus(uid, status, pr);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("content", p.getContent().stream().map(this::toSummary).toList());
        out.put("page", p.getNumber());
        out.put("size", p.getSize());
        out.put("total", p.getTotalElements());
        return out;
    }

    @GetMapping("/me/{maDh}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public Map<String, Object> myOrderDetail(@PathVariable String maDh, Authentication auth) {
        OrderEntity o = orders.findByMaDh(maDh).orElseThrow();
        if (!o.getCustomerId().equals(userId(auth))) {
            throw new org.springframework.security.access.AccessDeniedException("Not your order");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        // Flat fields for FE consumption
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
        out.put("events", events.findByOrderIdOrderBySequenceNoAsc(o.getId()).stream()
                .map(this::toEventView).toList());
        return out;
    }

    private OrderSummary toSummary(OrderEntity o) {
        return new OrderSummary(o.getId(), o.getMaDh(), o.getCurrentStatus().name(),
                o.getSubtotal(), o.getDiscountAmount(), o.getVatAmount(), o.getTotal(),
                o.getVoucherCode(),
                o.getDeliveryType() == null ? null : o.getDeliveryType().name(),
                o.getBranchId(), o.getCreatedAt());
    }

    private Map<String, Object> toEventView(OrderEvent e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("seq", e.getSequenceNo());
        m.put("type", e.getEventType());
        m.put("at", e.getOccurredAt());
        m.put("data", e.getEventData());
        return m;
    }
}
