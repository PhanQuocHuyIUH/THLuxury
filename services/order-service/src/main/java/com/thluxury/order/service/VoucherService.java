package com.thluxury.order.service;

import com.thluxury.order.domain.Voucher;
import com.thluxury.order.repository.VoucherRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Service
public class VoucherService {

    private final VoucherRepository vouchers;

    public VoucherService(VoucherRepository vouchers) {
        this.vouchers = vouchers;
    }

    public record VoucherResult(Voucher voucher, BigDecimal discountAmount) {}

    /** Validate + tính discount. Trả null nếu không áp dụng được. */
    public VoucherResult applyVoucher(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) return null;
        Voucher v = vouchers.findByCode(code.trim()).orElse(null);
        if (v == null || !v.isEnabled()) throw new IllegalArgumentException("VOUCHER_NOT_FOUND");
        if (v.getExpiresAt() != null && v.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("VOUCHER_EXPIRED");
        }
        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) {
            throw new IllegalArgumentException("VOUCHER_USED_UP");
        }
        if (subtotal.compareTo(v.getMinOrderValue()) < 0) {
            throw new IllegalArgumentException("VOUCHER_MIN_ORDER_NOT_MET");
        }
        BigDecimal discount;
        if (v.getType() == Voucher.Type.PERCENT) {
            discount = subtotal.multiply(v.getValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else { // FIXED
            discount = v.getValue().setScale(0, RoundingMode.HALF_UP);
        }
        if (v.getMaxDiscount() != null && discount.compareTo(v.getMaxDiscount()) > 0) {
            discount = v.getMaxDiscount();
        }
        if (discount.compareTo(subtotal) > 0) discount = subtotal;
        return new VoucherResult(v, discount);
    }

    public void incrementUsage(Voucher v) {
        v.setUsedCount(v.getUsedCount() + 1);
        vouchers.save(v);
    }
}
