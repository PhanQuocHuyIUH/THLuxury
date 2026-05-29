#!/usr/bin/env bash
# Smoke test Sprint 5 — Payment Saga full flow (PAID + FAILED compensate).
set -euo pipefail
GW="${GATEWAY:-http://localhost:8080}"

green() { printf "\033[32m✓\033[0m %s\n" "$*"; }
red()   { printf "\033[31m✗\033[0m %s\n" "$*"; }
hdr()   { printf "\n\033[36m── %s ──\033[0m\n" "$*"; }
pyx() { python -c "import sys,json; $1"; }

hdr "1. Login CUSTOMER + lấy IDs (pick product có stock đầy đủ — skip 3 sản phẩm mới tạo từ smoke S2)"
TOK=$(curl -fsS -X POST "$GW/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"an.nguyen@example.com","password":"Demo@123"}' \
  | pyx "print(json.load(sys.stdin)['accessToken'])")
# bỏ qua 3 product mới (maSp 31-33 do smoke-s2 add nhiều lần), dùng product index 3+ (original seeded có 5-30 stock).
P1=$(curl -fsS "$GW/api/products?size=10" | pyx "d=json.load(sys.stdin); print(d['content'][3]['id'])")
P2=$(curl -fsS "$GW/api/products?size=10" | pyx "d=json.load(sys.stdin); print(d['content'][4]['id'])")
BR=$(curl -fsS "$GW/api/branches" | pyx "d=json.load(sys.stdin); print([b['id'] for b in d if (b['code'] or '').upper()=='HCM'][0])")
green "OK p1=${P1:0:8} p2=${P2:0:8}"

hdr "2. Checkout method=COD (luôn PAID dù Mock PENDING)"
curl -fsS -X DELETE "$GW/api/cart" -H "Authorization: Bearer $TOK" > /dev/null
curl -fsS -X POST "$GW/api/cart/sync" -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' \
  -d "{\"items\":[{\"productId\":\"$P1\",\"quantity\":1}]}" > /dev/null
COD=$(curl -fsS -X POST "$GW/api/orders/checkout" -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' \
  -d "{\"deliveryType\":\"STORE_PICKUP\",\"branchId\":\"$BR\",\"paymentMethod\":\"COD\"}")
echo "$COD" | pyx "d=json.load(sys.stdin); print(f'  maDh={d[\"maDh\"]} status={d[\"currentStatus\"]} total={d[\"total\"]}')"
MADH_COD=$(echo "$COD" | pyx "print(json.load(sys.stdin)['maDh'])")

hdr "3. Detail + event timeline COD"
curl -fsS "$GW/api/orders/me/$MADH_COD" -H "Authorization: Bearer $TOK" \
  | pyx "d=json.load(sys.stdin); [print(f'  seq={e[\"seq\"]} {e[\"type\"]}') for e in d['events']]"

hdr "4. Checkout method=CREDIT_CARD nhiều lần — quan sát SUCCESS/FAILED mix"
echo "  failureRate hiện tại = $(docker exec thluxury-payment env 2>/dev/null | grep PAYMENT_MOCK_FAILURE_RATE || echo 'default 0.2')"
for i in 1 2 3 4 5; do
  curl -fsS -X DELETE "$GW/api/cart" -H "Authorization: Bearer $TOK" > /dev/null
  curl -fsS -X POST "$GW/api/cart/sync" -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' \
    -d "{\"items\":[{\"productId\":\"$P2\",\"quantity\":1}]}" > /dev/null
  RESP=$(curl -fsS -X POST "$GW/api/orders/checkout" -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' \
    -d "{\"deliveryType\":\"STORE_PICKUP\",\"branchId\":\"$BR\",\"paymentMethod\":\"CREDIT_CARD\"}")
  echo "$RESP" | pyx "d=json.load(sys.stdin); print(f'  try#${i}: maDh={d[\"maDh\"]} status={d[\"currentStatus\"]}')"
done

hdr "5. Liệt kê payments (ADMIN)"
ADMIN=$(curl -fsS -X POST "$GW/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"admin@thluxury.local","password":"Demo@123"}' \
  | pyx "print(json.load(sys.stdin)['accessToken'])")
curl -fsS "$GW/api/payments" -H "Authorization: Bearer $ADMIN" \
  | pyx "d=json.load(sys.stdin); print(f'  total payments={len(d)}'); [print(f'  - {p[\"method\"]:12s} {p[\"status\"]:8s} amount={p[\"amount\"]:>12} ref={(p.get(\"gatewayRef\") or \"\")[:20]}') for p in d[:10]]"

hdr "6. Reserved_quantity sau FAIL phải được Inventory release"
docker exec thluxury-postgres psql -U postgres -d thluxury -c "SELECT SUM(reserved_quantity) AS total_reserved FROM inventory.inventory;" 2>/dev/null || true

hdr "7. order.events + payment.events exchange + queue inventory commit"
docker exec thluxury-rabbitmq rabbitmqctl --quiet list_exchanges name type 2>/dev/null | grep -E "order|payment" || true
docker exec thluxury-rabbitmq rabbitmqctl --quiet list_queues name messages consumers 2>/dev/null | grep -E "inventory|^name" || true

hdr "8. Inventory đã commit sau order.paid? (movements type=COMMIT)"
docker exec thluxury-postgres psql -U postgres -d thluxury -c "SELECT type, COUNT(*) FROM inventory.inventory_movements GROUP BY type;" 2>/dev/null || true

hdr "9. Test orders/me — phân bố status"
curl -fsS "$GW/api/orders/me?size=20" -H "Authorization: Bearer $TOK" \
  | pyx "d=json.load(sys.stdin); from collections import Counter; c=Counter(o['currentStatus'] for o in d['content']); print(f'  total={d[\"total\"]} dist={dict(c)}')"

green "Smoke test S5 done"
