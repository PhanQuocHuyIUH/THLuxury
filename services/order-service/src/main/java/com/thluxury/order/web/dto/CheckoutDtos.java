package com.thluxury.order.web.dto;

import com.thluxury.order.domain.DeliveryType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class CheckoutDtos {
    private CheckoutDtos() {}

    public record CustomerInput(
            @Size(max = 200) String fullName,
            @Size(max = 20)  String phone,
            @Size(max = 255) String email
    ) {}

    public record AddressInput(
            @Size(max = 300) String diaChi,
            @Size(max = 100) String thanhPho,
            @Size(max = 100) String quan,
            @Size(max = 100) String phuong
    ) {}

    public record CheckoutRequest(
            @NotNull DeliveryType deliveryType,
            UUID branchId,                   // bắt buộc nếu STORE_PICKUP
            AddressInput address,            // bắt buộc nếu HOME_DELIVERY
            String voucherCode,
            String paymentMethod,            // COD | BANK_TRANSFER | CREDIT_CARD
            CustomerInput customer           // override snapshot (optional)
    ) {}

    public record OrderSummary(
            UUID id,
            String maDh,
            String currentStatus,
            java.math.BigDecimal subtotal,
            java.math.BigDecimal discountAmount,
            java.math.BigDecimal vatAmount,
            java.math.BigDecimal total,
            String voucherCode,
            String deliveryType,
            UUID branchId,
            java.time.Instant createdAt
    ) {}
}
