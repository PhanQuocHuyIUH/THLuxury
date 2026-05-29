package com.thluxury.order.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.thluxury.order.service.CartService;
import com.thluxury.order.service.CatalogClient;
import com.thluxury.order.web.dto.CartDtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService carts;
    private final CatalogClient catalog;

    public CartController(CartService carts, CatalogClient catalog) {
        this.carts = carts;
        this.catalog = catalog;
    }

    private UUID userId(Authentication auth) {
        return UUID.fromString(((Jwt) auth.getPrincipal()).getSubject());
    }

    @GetMapping
    public CartView get(Authentication auth) {
        Map<UUID, Integer> cart = carts.get(userId(auth));
        return enrich(cart);
    }

    @PostMapping("/sync")
    public CartView sync(@RequestBody SyncRequest req, Authentication auth) {
        Map<UUID, Integer> incoming = new HashMap<>();
        if (req != null && req.items() != null) {
            for (SyncItem it : req.items()) incoming.merge(it.productId(), it.quantity(), Integer::sum);
        }
        return enrich(carts.sync(userId(auth), incoming));
    }

    @PutMapping("/items/{productId}")
    public CartView setItem(@PathVariable UUID productId,
                            @RequestBody SetItemRequest req,
                            Authentication auth) {
        UUID uid = userId(auth);
        carts.setItem(uid, productId, req.quantity());
        return enrich(carts.get(uid));
    }

    @DeleteMapping("/items/{productId}")
    public CartView removeItem(@PathVariable UUID productId, Authentication auth) {
        UUID uid = userId(auth);
        carts.removeItem(uid, productId);
        return enrich(carts.get(uid));
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(Authentication auth) {
        carts.clear(userId(auth));
        return ResponseEntity.noContent().build();
    }

    private CartView enrich(Map<UUID, Integer> cart) {
        List<CartItemDto> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<UUID, Integer> e : cart.entrySet()) {
            JsonNode p;
            try { p = catalog.getProduct(e.getKey()); } catch (Exception ex) { continue; }
            if (p == null) continue;
            BigDecimal price = p.hasNonNull("giaHienTai")
                    ? new BigDecimal(p.get("giaHienTai").asText())
                    : p.hasNonNull("giaBanDau") ? new BigDecimal(p.get("giaBanDau").asText()) : BigDecimal.ZERO;
            String tenSp = p.hasNonNull("tenSp") ? p.get("tenSp").asText() : null;
            String hinh = null;
            JsonNode imgs = p.get("images");
            if (imgs != null && imgs.isArray() && imgs.size() > 0) {
                JsonNode first = imgs.get(0);
                hinh = first.has("imageUrl") ? first.get("imageUrl").asText()
                        : (first.isTextual() ? first.asText() : null);
            }
            items.add(new CartItemDto(e.getKey(), e.getValue(), tenSp, hinh, price));
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(e.getValue())));
        }
        return new CartView(items, subtotal);
    }
}
