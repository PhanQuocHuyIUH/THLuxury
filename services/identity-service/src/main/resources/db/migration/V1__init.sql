-- ============================================================
-- Identity Service — initial schema
-- Chạy trong schema "identity" (search_path đã set ở role svc_identity).
-- ============================================================

-- ---------- branches ----------
CREATE TABLE IF NOT EXISTS branches (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(16)  UNIQUE NOT NULL,
    name        VARCHAR(200) NOT NULL,
    address     VARCHAR(500),
    city        VARCHAR(100),
    district    VARCHAR(100),
    ward        VARCHAR(100),
    phone       VARCHAR(20),
    lat         NUMERIC(9,6),
    lng         NUMERIC(9,6),
    enabled     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- users ----------
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) UNIQUE NOT NULL,
    password_hash   VARCHAR(255),
    full_name       VARCHAR(200) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER'
                    CHECK (role IN ('CUSTOMER','BRANCH_MANAGER','ADMIN')),
    branch_id       UUID REFERENCES branches(id),
    oauth_provider  VARCHAR(20),
    oauth_subject   VARCHAR(255),
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    version         BIGINT  NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_users_oauth UNIQUE (oauth_provider, oauth_subject),
    CONSTRAINT chk_users_branch_role
        CHECK (
            (role = 'BRANCH_MANAGER' AND branch_id IS NOT NULL)
            OR (role <> 'BRANCH_MANAGER')
        )
);

CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);
CREATE INDEX IF NOT EXISTS idx_users_branch ON users(branch_id);

-- Trigger updated_at
CREATE OR REPLACE FUNCTION identity.touch_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION identity.touch_updated_at();

DROP TRIGGER IF EXISTS trg_branches_updated_at ON branches;
CREATE TRIGGER trg_branches_updated_at BEFORE UPDATE ON branches
    FOR EACH ROW EXECUTE FUNCTION identity.touch_updated_at();
