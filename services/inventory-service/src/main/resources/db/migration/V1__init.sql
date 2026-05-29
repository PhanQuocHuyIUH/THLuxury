-- ============================================================
-- Inventory Service — initial schema
-- Plan §3.3: inventory + inventory_movements
-- Search path đã được set thành "inventory" cho role svc_inventory.
-- ============================================================

-- ---------- inventory ----------
CREATE TABLE IF NOT EXISTS inventory (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id          UUID NOT NULL,
    branch_id           UUID NOT NULL,
    quantity            INT  NOT NULL DEFAULT 0,
    reserved_quantity   INT  NOT NULL DEFAULT 0,
    version             BIGINT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_product_branch UNIQUE (product_id, branch_id)
);

-- CHECK ràng buộc lượng đã reserve không âm và không vượt tồn kho.
DO $$ BEGIN
    ALTER TABLE inventory ADD CONSTRAINT inventory_reserved_chk
        CHECK (reserved_quantity >= 0 AND reserved_quantity <= quantity);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    ALTER TABLE inventory ADD CONSTRAINT inventory_quantity_chk
        CHECK (quantity >= 0);
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

CREATE INDEX IF NOT EXISTS idx_inventory_product ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_branch  ON inventory(branch_id);
CREATE INDEX IF NOT EXISTS idx_inventory_low_stock
    ON inventory ((quantity - reserved_quantity));

-- ---------- inventory_movements ----------
CREATE TABLE IF NOT EXISTS inventory_movements (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID NOT NULL,
    branch_id   UUID NOT NULL,
    type        VARCHAR(20) NOT NULL
                CHECK (type IN ('STOCK_IN','RESERVE','RELEASE','COMMIT','ADJUST')),
    quantity    INT NOT NULL,
    reference   VARCHAR(100),
    created_by  UUID,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_movements_product_branch_ts
    ON inventory_movements(product_id, branch_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_movements_reference
    ON inventory_movements(reference);
CREATE INDEX IF NOT EXISTS idx_movements_type
    ON inventory_movements(type);

-- Trigger updated_at on inventory
CREATE OR REPLACE FUNCTION inventory.touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_inventory_updated_at ON inventory;
CREATE TRIGGER trg_inventory_updated_at BEFORE UPDATE ON inventory
    FOR EACH ROW EXECUTE FUNCTION inventory.touch_updated_at();
