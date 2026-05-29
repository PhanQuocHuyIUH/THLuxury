package com.thluxury.order.web;

import com.thluxury.order.domain.Voucher;
import com.thluxury.order.repository.VoucherRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    private final VoucherRepository vouchers;

    public VoucherController(VoucherRepository vouchers) {
        this.vouchers = vouchers;
    }

    public record VoucherView(UUID id, String code, String type, BigDecimal value,
                              BigDecimal minOrderValue, BigDecimal maxDiscount,
                              Instant expiresAt, Integer usageLimit, int usedCount,
                              boolean enabled) {}

    public record CreateVoucherRequest(@NotBlank String code, @NotNull Voucher.Type type,
                                       @NotNull BigDecimal value, BigDecimal minOrderValue,
                                       BigDecimal maxDiscount, Instant expiresAt,
                                       Integer usageLimit, Boolean enabled) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','BRANCH_MANAGER')")
    public List<VoucherView> list() {
        return vouchers.findAll().stream().map(this::toView).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<VoucherView> create(@RequestBody CreateVoucherRequest req) {
        Voucher v = new Voucher();
        v.setCode(req.code().trim());
        v.setType(req.type());
        v.setValue(req.value());
        v.setMinOrderValue(req.minOrderValue() == null ? BigDecimal.ZERO : req.minOrderValue());
        v.setMaxDiscount(req.maxDiscount());
        v.setExpiresAt(req.expiresAt());
        v.setUsageLimit(req.usageLimit());
        v.setEnabled(req.enabled() == null || req.enabled());
        v = vouchers.save(v);
        return ResponseEntity.status(HttpStatus.CREATED).body(toView(v));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vouchers.findById(id).ifPresent(v -> { v.setEnabled(false); vouchers.save(v); });
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{code}/validate")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<VoucherView> validate(@PathVariable String code) {
        Voucher v = vouchers.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("VOUCHER_NOT_FOUND"));
        if (!v.isEnabled()) throw new IllegalArgumentException("VOUCHER_DISABLED");
        if (v.getExpiresAt() != null && v.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("VOUCHER_EXPIRED");
        }
        return ResponseEntity.ok(toView(v));
    }

    private VoucherView toView(Voucher v) {
        return new VoucherView(v.getId(), v.getCode(), v.getType().name(), v.getValue(),
                v.getMinOrderValue(), v.getMaxDiscount(), v.getExpiresAt(),
                v.getUsageLimit(), v.getUsedCount(), v.isEnabled());
    }
}
