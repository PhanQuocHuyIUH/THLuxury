-- ============================================================
-- THLuxury — Create per-service DB roles (least privilege)
-- Chạy 1 lần khi container postgres khởi tạo (docker-entrypoint-initdb.d).
-- Password được override bằng biến môi trường lấy từ .env qua 03-set-passwords.sh.
-- ============================================================

\set ON_ERROR_STOP on

CREATE ROLE svc_identity  LOGIN PASSWORD 'svc_identity_pw';
CREATE ROLE svc_catalog   LOGIN PASSWORD 'svc_catalog_pw';
CREATE ROLE svc_inventory LOGIN PASSWORD 'svc_inventory_pw';
CREATE ROLE svc_order     LOGIN PASSWORD 'svc_order_pw';
CREATE ROLE svc_payment   LOGIN PASSWORD 'svc_payment_pw';

-- Tất cả service đều cần connect vào db chính
GRANT CONNECT ON DATABASE thluxury TO
    svc_identity, svc_catalog, svc_inventory, svc_order, svc_payment;
