package com.thluxury.order.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.thluxury.order.domain.*;
import com.thluxury.order.grpc.InventoryGrpcClient;
import com.thluxury.order.grpc.PaymentGrpcClient;
import com.thluxury.order.messaging.OrderEventPublisher;
import com.thluxury.order.repository.OrderEventRepository;
import com.thluxury.order.repository.OrderRepository;
import com.thluxury.order.repository.TaxConfigRepository;
import com.thluxury.order.service.CartService;
import com.thluxury.order.service.CatalogClient;
import com.thluxury.order.service.VoucherService;
import com.thluxury.order.web.dto.CheckoutDtos.CheckoutRequest;
import com.thluxury.order.web.dto.CheckoutDtos.CustomerInput;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Saga Orchestrator — Sprint 4 implement đến PRICED.
 *  Step 1: ORDER_CREATED  (persist + event)
 *  Step 2: INVENTORY_RESERVED  (gRPC)
 *  Step 3: PRICING_APPLIED  (voucher + VAT)
 *  Step 4–5 (PAYMENT) sẽ thêm ở Sprint 5.
 */
@Service
public class OrderSagaService {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaService.class);
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // bỏ ký tự dễ nhầm
    private static final SecureRandom RND = new SecureRandom();

    private final OrderRepository orders;
    private final OrderEventRepository events;
    private final TaxConfigRepository taxConfigs;
    private final CartService carts;
    private final CatalogClient catalog;
    private final VoucherService vouchers;
    private final InventoryGrpcClient inventory;
    private final PaymentGrpcClient payment;
    private final ObjectMapper json;
    private final OrderEventPublisher publisher;
    private final MeterRegistry metrics;
    private final boolean reserveStrictBranch;

    public OrderSagaService(OrderRepository orders,
                            OrderEventRepository events,
                            TaxConfigRepository taxConfigs,
                            CartService carts,
                            CatalogClient catalog,
                            VoucherService vouchers,
                            InventoryGrpcClient inventory,
                            PaymentGrpcClient payment,
                            ObjectMapper json,
                            OrderEventPublisher publisher,
                            MeterRegistry metrics,
                            @Value("${thluxury.order.saga.reserve-strict-branch:true}") boolean strict) {
        this.orders = orders;
        this.events = events;
        this.taxConfigs = taxConfigs;
        this.carts = carts;
        this.catalog = catalog;
        this.vouchers = vouchers;
        this.inventory = inventory;
        this.payment = payment;
        this.json = json;
        this.publisher = publisher;
        this.metrics = metrics;
        this.reserveStrictBranch = strict;
    }

    @Transactional
    public OrderEntity checkout(UUID userId, CustomerInput customer, CheckoutRequest req) {
        Map<UUID, Integer> cart = carts.get(userId);
        if (cart.isEmpty()) {
            throw new IllegalStateException("CART_EMPTY");
        }

        // --- Build item snapshot + subtotal từ Catalog (giá hiện tại) ---
        ArrayNode itemsArr = json.createArrayNode();
        BigDecimal subtotal = BigDecimal.ZERO;
        List<InventoryGrpcClient.ReserveItem> reserveItems = new ArrayList<>();

        for (Map.Entry<UUID, Integer> e : cart.entrySet()) {
            UUID productId = e.getKey();
            int qty = e.getValue();
            JsonNode product;
            try {
                product = catalog.getProduct(productId);
            } catch (Exception ex) {
                throw new IllegalStateException("PRODUCT_NOT_FOUND:" + productId);
            }
            if (product == null) throw new IllegalStateException("PRODUCT_NOT_FOUND:" + productId);

            BigDecimal price = bd(product, "giaHienTai");
            if (price == null) price = bd(product, "giaBanDau");
            if (price == null) throw new IllegalStateException("PRODUCT_PRICE_MISSING:" + productId);

            ObjectNode item = json.createObjectNode();
            item.put("productId", productId.toString());
            if (product.hasNonNull("maSp")) item.put("maSp", product.get("maSp").asLong());
            item.put("tenSp", text(product, "tenSp"));
            item.put("hinh", firstImage(product));
            item.put("soLuong", qty);
            item.put("giaMua", price);
            itemsArr.add(item);

            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(qty)));

            UUID branch = req.branchId();
            reserveItems.add(new InventoryGrpcClient.ReserveItem(
                    productId,
                    req.deliveryType() == DeliveryType.STORE_PICKUP ? branch :
                            (reserveStrictBranch ? branch : null),
                    qty));
        }

        // --- Validate STORE_PICKUP cần branchId, HOME_DELIVERY cần address ---
        if (req.deliveryType() == DeliveryType.STORE_PICKUP && req.branchId() == null) {
            throw new IllegalArgumentException("BRANCH_ID_REQUIRED_FOR_PICKUP");
        }
        if (req.deliveryType() == DeliveryType.HOME_DELIVERY && req.address() == null) {
            throw new IllegalArgumentException("ADDRESS_REQUIRED_FOR_DELIVERY");
        }

        // --- Step 1: ORDER_CREATED ---
        OrderEntity order = new OrderEntity();
        order.setMaDh(generateMaDh());
        order.setCustomerId(userId);
        order.setCustomerSnapshot(writeJson(customer));
        order.setDeliveryType(req.deliveryType());
        order.setBranchId(req.branchId() != null ? req.branchId() :
                resolveBranchFallback(reserveItems));
        order.setAddressSnapshot(req.address() == null ? null : writeJson(req.address()));
        order.setItemsSnapshot(itemsArr.toString());
        order.setSubtotal(subtotal);
        order.setVatPercent(BigDecimal.ZERO);
        order.setVatAmount(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotal(subtotal);
        order.setPaymentMethod(req.paymentMethod() == null ? "COD" : req.paymentMethod());
        order.setCurrentStatus(OrderStatus.CREATED);
        order = orders.save(order);

        appendEvent(order.getId(), 1, "ORDER_CREATED", Map.of(
                "maDh", order.getMaDh(),
                "customerId", userId.toString(),
                "itemCount", itemsArr.size(),
                "subtotal", subtotal.toPlainString()));
        publisher.publishCreated(order);

        // --- Step 2: INVENTORY_RESERVED (gRPC sync) ---
        InventoryGrpcClient.ReserveOutcome outcome;
        try {
            outcome = inventory.reserve(order.getId(), reserveItems, reserveStrictBranch);
        } catch (Exception e) {
            log.error("Inventory.reserve gRPC failed", e);
            metrics.counter("inventory.reserve.failures", "reason", "INVENTORY_UNAVAILABLE").increment();
            failOrder(order, "INVENTORY_UNAVAILABLE", e.getMessage());
            throw new IllegalStateException("INVENTORY_UNAVAILABLE");
        }
        if (!outcome.success()) {
            metrics.counter("inventory.reserve.failures",
                    "reason", outcome.errorCode() == null ? "INSUFFICIENT_STOCK" : outcome.errorCode())
                    .increment();
            failOrder(order, "INVENTORY_RESERVE_FAILED", outcome.errorCode());
            throw new IllegalStateException("INSUFFICIENT_STOCK");
        }
        appendEvent(order.getId(), 2, "INVENTORY_RESERVED", Map.of(
                "items", outcome.itemResults().stream().map(r -> Map.of(
                        "productId", r.getProductId(),
                        "branchId",  r.getBranchId(),
                        "reserved",  r.getReserved())).toList()));
        order.setCurrentStatus(OrderStatus.RESERVED);
        orders.save(order);

        // --- Step 3: PRICING_APPLIED (voucher + VAT) ---
        VoucherService.VoucherResult vr = null;
        try {
            vr = vouchers.applyVoucher(req.voucherCode(), subtotal);
        } catch (IllegalArgumentException ex) {
            // voucher invalid: compensate Inventory + fail order
            inventory.release(order.getId(), "VOUCHER_INVALID");
            failOrder(order, "VOUCHER_INVALID", ex.getMessage());
            throw ex;
        }
        BigDecimal discount = vr == null ? BigDecimal.ZERO : vr.discountAmount();

        BigDecimal vatPercent = taxConfigs.findByCode("VAT")
                .map(TaxConfig::getPercent).orElse(BigDecimal.TEN);
        BigDecimal afterDiscount = subtotal.subtract(discount);
        BigDecimal vatAmount = afterDiscount.multiply(vatPercent)
                .divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);
        BigDecimal total = afterDiscount.add(vatAmount);

        order.setVoucherCode(vr == null ? null : vr.voucher().getCode());
        order.setDiscountAmount(discount);
        order.setVatPercent(vatPercent);
        order.setVatAmount(vatAmount);
        order.setTotal(total);
        order.setCurrentStatus(OrderStatus.PRICED);
        orders.save(order);

        appendEvent(order.getId(), 3, "PRICING_APPLIED", Map.of(
                "discount", discount.toPlainString(),
                "vatPercent", vatPercent.toPlainString(),
                "vatAmount", vatAmount.toPlainString(),
                "total", total.toPlainString(),
                "voucher", order.getVoucherCode() == null ? "" : order.getVoucherCode()));

        if (vr != null) vouchers.incrementUsage(vr.voucher());

        // Cart clear ngay sau PRICED để tránh tạo order trùng khi reload.
        carts.clear(userId);

        // --- Step 4: PAYMENT_INITIATED ---
        appendEvent(order.getId(), 4, "PAYMENT_INITIATED", Map.of(
                "method", order.getPaymentMethod(),
                "amount", total.toPlainString()));

        PaymentGrpcClient.ChargeOutcome chargeOutcome;
        try {
            chargeOutcome = payment.charge(order.getId(), order.getMaDh(),
                    order.getPaymentMethod(), total, userId.toString(),
                    "order-" + order.getId());
        } catch (Exception e) {
            log.error("Payment.charge gRPC failed", e);
            inventory.release(order.getId(), "PAYMENT_GATEWAY_ERROR");
            failOrder(order, "PAYMENT_GATEWAY_ERROR", e.getMessage());
            throw new IllegalStateException("PAYMENT_GATEWAY_ERROR");
        }

        // --- Step 5a/5b: PAYMENT_SUCCESS / ORDER_CONFIRMED / PAYMENT_FAILED ---
        String status = chargeOutcome.status();
        boolean isCod = "COD".equalsIgnoreCase(order.getPaymentMethod());
        if ("SUCCESS".equals(status)) {
            // Thanh toán online đã capture → PAID.
            appendEvent(order.getId(), 5, "PAYMENT_SUCCESS", Map.of(
                    "paymentId", safe(chargeOutcome.paymentId()),
                    "gatewayRef", safe(chargeOutcome.gatewayRef()),
                    "status", status));
            order.setCurrentStatus(OrderStatus.PAID);
            orders.save(order);
            // publishPaid → Inventory commit kho + Notification "đặt hàng thành công".
            publisher.publishPaid(order);
            metrics.counter("orders.created",
                    "branch", order.getBranchId().toString(), "status", "PAID").increment();
        } else if ("PENDING".equals(status) && isCod) {
            // COD: đơn đã chốt nhưng CHƯA thu tiền → CONFIRMED (không phải PAID).
            // Vẫn commit kho vì hàng đã được phân bổ cho khách; thu tiền khi giao/nhận.
            appendEvent(order.getId(), 5, "ORDER_CONFIRMED", Map.of(
                    "paymentId", safe(chargeOutcome.paymentId()),
                    "method", order.getPaymentMethod(),
                    "status", status));
            order.setCurrentStatus(OrderStatus.CONFIRMED);
            orders.save(order);
            publisher.publishPaid(order);
            metrics.counter("orders.created",
                    "branch", order.getBranchId().toString(), "status", "CONFIRMED").increment();
        } else {
            // FAILED → compensate Inventory + fail order.
            appendEvent(order.getId(), 5, "PAYMENT_FAILED", Map.of(
                    "paymentId", safe(chargeOutcome.paymentId()),
                    "errorCode", safe(chargeOutcome.errorCode()),
                    "errorMessage", safe(chargeOutcome.errorMessage())));
            inventory.release(order.getId(), "PAYMENT_FAILED");
            failOrder(order, "PAYMENT_FAILED",
                    chargeOutcome.errorCode() == null ? "Gateway declined" : chargeOutcome.errorCode());
            // KHÔNG throw — trả về order với status FAILED để FE hiển thị lý do.
            return order;
        }

        return order;
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private UUID resolveBranchFallback(List<InventoryGrpcClient.ReserveItem> items) {
        // Fallback (HOME_DELIVERY không branchId): chọn branch đầu tiên trong items có giá trị,
        // hoặc UUID(0) placeholder để DB chấp nhận.
        return items.stream()
                .map(InventoryGrpcClient.ReserveItem::branchId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(new UUID(0L, 0L));
    }

    private void failOrder(OrderEntity order, String reason, String detail) {
        order.setCurrentStatus(OrderStatus.FAILED);
        order.setFailureReason((reason + ":" + (detail == null ? "" : detail))
                .substring(0, Math.min(500, (reason + ":" + (detail == null ? "" : detail)).length())));
        orders.save(order);
        appendEvent(order.getId(),
                (int) (events.countByOrderId(order.getId()) + 1),
                "ORDER_FAILED", Map.of("reason", reason, "detail", detail == null ? "" : detail));
        publisher.publishFailed(order, reason);
        metrics.counter("orders.created",
                "branch", order.getBranchId().toString(), "status", "FAILED").increment();
    }

    private void appendEvent(UUID orderId, int seq, String type, Map<String, ?> data) {
        events.save(new OrderEvent(orderId, seq, type, writeJson(data)));
    }

    private String writeJson(Object o) {
        try { return json.writeValueAsString(o); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    private String generateMaDh() {
        StringBuilder sb = new StringBuilder("DH");
        for (int i = 0; i < 8; i++) sb.append(ALPHABET.charAt(RND.nextInt(ALPHABET.length())));
        return sb.toString();
    }

    private static BigDecimal bd(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v == null || v.isNull() ? null : new BigDecimal(v.asText());
    }
    private static String text(JsonNode n, String f) {
        JsonNode v = n.get(f);
        return v == null || v.isNull() ? null : v.asText();
    }
    private static String firstImage(JsonNode n) {
        JsonNode arr = n.get("images");
        if (arr != null && arr.isArray() && arr.size() > 0) {
            JsonNode first = arr.get(0);
            if (first.isObject() && first.has("imageUrl")) return first.get("imageUrl").asText();
            if (first.isTextual()) return first.asText();
        }
        return null;
    }
}
