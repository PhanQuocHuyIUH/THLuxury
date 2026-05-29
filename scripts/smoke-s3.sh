#!/usr/bin/env bash
# Smoke test for Sprint 3 — Inventory Service via Gateway.
set -euo pipefail
GW="${GATEWAY:-http://localhost:8080}"

green() { printf "\033[32m✓\033[0m %s\n" "$*"; }
red()   { printf "\033[31m✗\033[0m %s\n" "$*"; }
hdr()   { printf "\n\033[36m── %s ──\033[0m\n" "$*"; }
pyx() { python -c "import sys,json; $1" ; }

hdr "1. Đăng nhập ADMIN"
ADMIN_TOKEN=$(curl -fsS -X POST "$GW/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"admin@thluxury.local","password":"Demo@123"}' \
  | pyx "print(json.load(sys.stdin)['accessToken'])")
green "ADMIN token OK"

hdr "2. Đăng nhập BRANCH_MANAGER (HCM)"
MGR_TOKEN=$(curl -fsS -X POST "$GW/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"manager.hcm@thluxury.local","password":"Demo@123"}' \
  | pyx "print(json.load(sys.stdin)['accessToken'])")
green "MANAGER token OK"

hdr "3. List inventory (ADMIN không filter branch → toàn bộ)"
curl -fsS "$GW/api/inventory" -H "Authorization: Bearer $ADMIN_TOKEN" \
  | pyx "d=json.load(sys.stdin); print(f'  rows={len(d)}'); print(f'  sample[0]={d[0] if d else None}')"

hdr "4. List inventory (MANAGER HCM → tự bị filter branch HCM)"
curl -fsS "$GW/api/inventory" -H "Authorization: Bearer $MGR_TOKEN" \
  | pyx "d=json.load(sys.stdin); print(f'  rows={len(d)}'); bs=set(x[\"branchId\"] for x in d); print(f'  unique branches in response = {len(bs)} (expected 1)')"

hdr "5. Low-stock endpoint (threshold=10)"
curl -fsS "$GW/api/inventory/low-stock?threshold=10" -H "Authorization: Bearer $ADMIN_TOKEN" \
  | pyx "d=json.load(sys.stdin); print(f'  rows={len(d)}'); [print(f'  - product={x[\"productId\"][:8]}… branch={x[\"branchId\"][:8]}… avail={x[\"quantity\"]-x[\"reservedQuantity\"]}') for x in d[:3]]"

hdr "6. Stock-in (MANAGER HCM, branchId tự suy từ JWT)"
ANY_PRODUCT=$(curl -fsS "$GW/api/products?size=1" | pyx "print(json.load(sys.stdin)['content'][0]['id'])")
echo "  picked productId=$ANY_PRODUCT"
HTTP=$(curl -s -o /tmp/sin.txt -w "%{http_code}" -X POST "$GW/api/inventory/stock-in" \
  -H "Authorization: Bearer $MGR_TOKEN" -H 'Content-Type: application/json' \
  -d "{\"productId\":\"$ANY_PRODUCT\",\"quantity\":3,\"reference\":\"SMOKE-S3\"}")
echo "  HTTP $HTTP"

hdr "7. Movements list cho product này"
curl -fsS "$GW/api/inventory/movements?productId=$ANY_PRODUCT" -H "Authorization: Bearer $MGR_TOKEN" \
  | pyx "d=json.load(sys.stdin); print(f'  rows={len(d)}'); [print(f'  - type={m[\"type\"]} qty={m[\"quantity\"]} ref={m[\"reference\"]}') for m in d[:5]]"

hdr "8. RabbitMQ queue inventory.order-paid.q tồn tại?"
docker exec thluxury-rabbitmq rabbitmqctl --quiet list_queues name messages consumers 2>/dev/null | grep -E "inventory|^name" || true

hdr "9. CUSTOMER bị 403 trên /api/inventory"
USER_TOKEN=$(curl -fsS -X POST "$GW/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"an.nguyen@example.com","password":"Demo@123"}' \
  | pyx "print(json.load(sys.stdin)['accessToken'])")
code=$(curl -s -o /dev/null -w "%{http_code}" "$GW/api/inventory" -H "Authorization: Bearer $USER_TOKEN")
echo "  HTTP $code (kỳ vọng 403)"

green "Smoke test S3 done"
