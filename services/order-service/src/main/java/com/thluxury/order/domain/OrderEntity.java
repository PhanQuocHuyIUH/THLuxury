package com.thluxury.order.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ma_dh", nullable = false, length = 20, unique = true)
    private String maDh;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "customer_snapshot", columnDefinition = "jsonb", nullable = false)
    private String customerSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", nullable = false, length = 20)
    private DeliveryType deliveryType;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address_snapshot", columnDefinition = "jsonb")
    private String addressSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items_snapshot", columnDefinition = "jsonb", nullable = false)
    private String itemsSnapshot;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal subtotal;

    @Column(name = "vat_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatPercent;

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal vatAmount;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal total;

    @Column(name = "payment_method", nullable = false, length = 20)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false, length = 30)
    private OrderStatus currentStatus = OrderStatus.CREATED;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getMaDh() { return maDh; }
    public void setMaDh(String maDh) { this.maDh = maDh; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getCustomerSnapshot() { return customerSnapshot; }
    public void setCustomerSnapshot(String s) { this.customerSnapshot = s; }
    public DeliveryType getDeliveryType() { return deliveryType; }
    public void setDeliveryType(DeliveryType d) { this.deliveryType = d; }
    public UUID getBranchId() { return branchId; }
    public void setBranchId(UUID branchId) { this.branchId = branchId; }
    public String getAddressSnapshot() { return addressSnapshot; }
    public void setAddressSnapshot(String s) { this.addressSnapshot = s; }
    public String getItemsSnapshot() { return itemsSnapshot; }
    public void setItemsSnapshot(String s) { this.itemsSnapshot = s; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal v) { this.subtotal = v; }
    public BigDecimal getVatPercent() { return vatPercent; }
    public void setVatPercent(BigDecimal v) { this.vatPercent = v; }
    public BigDecimal getVatAmount() { return vatAmount; }
    public void setVatAmount(BigDecimal v) { this.vatAmount = v; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String v) { this.voucherCode = v; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal v) { this.discountAmount = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { this.total = v; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String s) { this.paymentMethod = s; }
    public OrderStatus getCurrentStatus() { return currentStatus; }
    public void setCurrentStatus(OrderStatus s) { this.currentStatus = s; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String s) { this.failureReason = s; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
