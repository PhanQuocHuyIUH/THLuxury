-- ============================================================
-- V3 — Thêm trạng thái CONFIRMED cho đơn COD.
-- Đơn COD sau checkout = CONFIRMED (đã chốt, chưa thu tiền) thay vì PAID.
-- Tiền được thu khi giao/nhận → lúc đó manager chuyển sang COMPLETED.
-- ============================================================

ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_current_status_check;

ALTER TABLE orders ADD CONSTRAINT orders_current_status_check
    CHECK (current_status IN (
        'CREATED','RESERVED','PRICED','PAID','CONFIRMED','PREPARING',
        'READY_FOR_PICKUP','SHIPPING','DELIVERED','COMPLETED',
        'CANCELLED','FAILED'
    ));
