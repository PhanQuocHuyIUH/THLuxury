-- ============================================================
-- THLuxury — Create 5 isolated schemas + grants
-- Mỗi service chỉ có toàn quyền trên schema của mình.
-- Identity được phép READ branches từ chính nó, các service khác
-- KHÔNG truy cập cross-schema trực tiếp (giao tiếp qua API/event).
-- ============================================================

\set ON_ERROR_STOP on

-- Extensions dùng chung
CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pg_trgm";    -- fuzzy search cho Catalog

-- ---------- Schemas ----------
CREATE SCHEMA IF NOT EXISTS identity  AUTHORIZATION svc_identity;
CREATE SCHEMA IF NOT EXISTS catalog   AUTHORIZATION svc_catalog;
CREATE SCHEMA IF NOT EXISTS inventory AUTHORIZATION svc_inventory;
CREATE SCHEMA IF NOT EXISTS "order"   AUTHORIZATION svc_order;
CREATE SCHEMA IF NOT EXISTS payment   AUTHORIZATION svc_payment;

-- ---------- Default privileges trên schema của mình ----------
-- Khi service tạo bảng trong schema riêng, tự động owner = chính nó (đã set AUTHORIZATION).
-- Cấp thêm USAGE để các tool migration (Flyway) chạy được.

GRANT USAGE, CREATE ON SCHEMA identity  TO svc_identity;
GRANT USAGE, CREATE ON SCHEMA catalog   TO svc_catalog;
GRANT USAGE, CREATE ON SCHEMA inventory TO svc_inventory;
GRANT USAGE, CREATE ON SCHEMA "order"   TO svc_order;
GRANT USAGE, CREATE ON SCHEMA payment   TO svc_payment;

-- ---------- Search path mặc định cho mỗi role ----------
ALTER ROLE svc_identity  SET search_path TO identity,  public;
ALTER ROLE svc_catalog   SET search_path TO catalog,   public;
ALTER ROLE svc_inventory SET search_path TO inventory, public;
ALTER ROLE svc_order     SET search_path TO "order",   public;
ALTER ROLE svc_payment   SET search_path TO payment,   public;

-- ---------- Sanity ----------
DO $$
BEGIN
    RAISE NOTICE 'THLuxury schemas initialized: identity, catalog, inventory, order, payment';
END $$;
