#!/usr/bin/env bash
# ============================================================
# THLuxury — Seed orchestrator (placeholder cho Sprint 0)
# Sprint 1+: chạy `mvn flyway:migrate` cho từng service + load fixtures.
# Hiện tại chỉ verify schemas đã tồn tại.
# ============================================================
set -euo pipefail

CONTAINER="${POSTGRES_CONTAINER:-thluxury-postgres}"
DB="${POSTGRES_DB:-thluxury}"
USER="${POSTGRES_USER:-postgres}"

echo "→ Verifying schemas in container '${CONTAINER}'..."
docker exec -e PGPASSWORD="${POSTGRES_PASSWORD:-postgres}" "${CONTAINER}" \
    psql -U "${USER}" -d "${DB}" -At -c \
    "SELECT schema_name FROM information_schema.schemata WHERE schema_name IN ('identity','catalog','inventory','order','payment') ORDER BY schema_name;"

echo ""
echo "→ Verifying service roles..."
docker exec -e PGPASSWORD="${POSTGRES_PASSWORD:-postgres}" "${CONTAINER}" \
    psql -U "${USER}" -d "${DB}" -At -c \
    "SELECT rolname FROM pg_roles WHERE rolname LIKE 'svc_%' ORDER BY rolname;"

echo ""
echo "✓ Sprint 0 seed verification done."
echo "  (Real data seed sẽ chạy ở các sprint tiếp theo qua Flyway migrate.)"
