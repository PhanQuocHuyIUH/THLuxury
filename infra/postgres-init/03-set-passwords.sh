#!/usr/bin/env bash
# ============================================================
# THLuxury — Override service passwords từ env vars
# Postgres entrypoint chạy mọi file *.sh/*.sql trong /docker-entrypoint-initdb.d
# theo thứ tự tên file. File này chạy sau 01 và 02.
# ============================================================
set -euo pipefail

# Chỉ override nếu env var được set (cho phép leave default trong dev)
override_password() {
    local role="$1"
    local password_var="$2"
    local password="${!password_var:-}"
    if [[ -n "$password" ]]; then
        psql -v ON_ERROR_STOP=1 \
             --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
             -c "ALTER ROLE ${role} WITH PASSWORD '${password}';"
        echo "  ✓ Updated password for ${role}"
    fi
}

echo "→ Applying service DB passwords from environment..."
override_password svc_identity  DB_PASSWORD_IDENTITY
override_password svc_catalog   DB_PASSWORD_CATALOG
override_password svc_inventory DB_PASSWORD_INVENTORY
override_password svc_order     DB_PASSWORD_ORDER
override_password svc_payment   DB_PASSWORD_PAYMENT
echo "→ Done."
