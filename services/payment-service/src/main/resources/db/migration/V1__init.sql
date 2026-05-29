-- ============================================================
-- Payment Service — initial schema
-- Plan §3.5: payments + payment_events
-- ============================================================

CREATE TABLE IF NOT EXISTS payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id        UUID NOT NULL,
    provider        VARCHAR(20) NOT NULL CHECK (provider IN ('MOCK','STRIPE')),
    method          VARCHAR(20) NOT NULL CHECK (method IN ('COD','BANK_TRANSFER','CREDIT_CARD')),
    amount          NUMERIC(15,0) NOT NULL CHECK (amount >= 0),
    currency        VARCHAR(8)  NOT NULL DEFAULT 'VND',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','SUCCESS','FAILED','REFUNDED')),
    gateway_ref     VARCHAR(200),
    attempt_count   INT NOT NULL DEFAULT 0,
    error_message   TEXT,
    idempotency_key VARCHAR(120),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_payments_order ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE UNIQUE INDEX IF NOT EXISTS uq_payments_idempotency
    ON payments(idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS payment_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id   UUID NOT NULL,
    event_type   VARCHAR(50) NOT NULL,
    payload      JSONB,
    occurred_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_payment_events_payment ON payment_events(payment_id, occurred_at);

-- Trigger updated_at
CREATE OR REPLACE FUNCTION payment.touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_payments_updated_at ON payments;
CREATE TRIGGER trg_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION payment.touch_updated_at();
