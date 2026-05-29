-- ============================================================
-- Order Service — initial schema (Event Sourcing + projection)
-- Plan §3.4: orders + order_events + vouchers + tax_config
-- ============================================================

-- ---------- orders (aggregate snapshot / projection) ----------
CREATE TABLE IF NOT EXISTS orders (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ma_dh               VARCHAR(20) NOT NULL UNIQUE,
    customer_id         UUID NOT NULL,
    customer_snapshot   JSONB NOT NULL,
    delivery_type       VARCHAR(20) NOT NULL
                        CHECK (delivery_type IN ('STORE_PICKUP','HOME_DELIVERY')),
    branch_id           UUID NOT NULL,
    address_snapshot    JSONB,
    items_snapshot      JSONB NOT NULL,
    subtotal            NUMERIC(15,0) NOT NULL,
    vat_percent         NUMERIC(5,2)  NOT NULL,
    vat_amount          NUMERIC(15,0) NOT NULL,
    voucher_code        VARCHAR(50),
    discount_amount     NUMERIC(15,0) NOT NULL DEFAULT 0,
    total               NUMERIC(15,0) NOT NULL,
    payment_method      VARCHAR(20) NOT NULL,
    current_status      VARCHAR(30) NOT NULL
                        CHECK (current_status IN (
                            'CREATED','RESERVED','PRICED','PAID','PREPARING',
                            'READY_FOR_PICKUP','SHIPPING','DELIVERED','COMPLETED',
                            'CANCELLED','FAILED'
                        )),
    failure_reason      VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_orders_branch_status ON orders(branch_id, current_status);
CREATE INDEX IF NOT EXISTS idx_orders_status_created ON orders(current_status, created_at DESC);

-- ---------- order_events (Event Store - source of truth) ----------
CREATE TABLE IF NOT EXISTS order_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID NOT NULL,
    sequence_no  INT  NOT NULL,
    event_type   VARCHAR(50) NOT NULL,
    event_data   JSONB NOT NULL,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_order_event_seq UNIQUE (order_id, sequence_no)
);
CREATE INDEX IF NOT EXISTS idx_order_events_order ON order_events(order_id, sequence_no);
CREATE INDEX IF NOT EXISTS idx_order_events_type ON order_events(event_type);

-- ---------- vouchers ----------
CREATE TABLE IF NOT EXISTS vouchers (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code              VARCHAR(50) NOT NULL UNIQUE,
    type              VARCHAR(20) NOT NULL CHECK (type IN ('PERCENT','FIXED')),
    value             NUMERIC(15,2) NOT NULL,
    min_order_value   NUMERIC(15,0) NOT NULL DEFAULT 0,
    max_discount      NUMERIC(15,0),
    expires_at        TIMESTAMPTZ,
    usage_limit       INT,
    used_count        INT NOT NULL DEFAULT 0,
    enabled           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_vouchers_code ON vouchers(code) WHERE enabled = TRUE;

-- ---------- tax_config ----------
CREATE TABLE IF NOT EXISTS tax_config (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(20) NOT NULL UNIQUE,
    percent         NUMERIC(5,2) NOT NULL,
    effective_from  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- Trigger updated_at on orders ----------
CREATE OR REPLACE FUNCTION "order".touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_orders_updated_at ON orders;
CREATE TRIGGER trg_orders_updated_at BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION "order".touch_updated_at();
