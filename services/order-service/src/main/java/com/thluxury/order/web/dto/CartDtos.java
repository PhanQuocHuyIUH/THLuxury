package com.thluxury.order.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public final class CartDtos {
    private CartDtos() {}

    public record CartItemDto(UUID productId, int quantity, String tenSp, String hinh, java.math.BigDecimal giaHienTai) {}

    public record CartView(List<CartItemDto> items, java.math.BigDecimal subtotal) {}

    public record SyncRequest(@NotNull List<@jakarta.validation.constraints.NotNull SyncItem> items) {}

    public record SyncItem(@NotNull UUID productId, @Min(1) int quantity) {}

    public record SetItemRequest(@Min(0) int quantity) {}
}
