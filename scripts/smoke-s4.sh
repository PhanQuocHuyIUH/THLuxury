#!/usr/bin/env bash
# Smoke test for Sprint 4 — Cart + Checkout Saga (steps 1-3).
set -euo pipefail
GW="${GATEWAY:-http://localhost:8080}"

green() { printf "\033[32m✓\033[0m %s\n" "$*"; }
red()   { printf "\033[31m✗\033[0m %s\n" "$*"; }
hdr()   { printf "\n\033[36m── %s ──\033[0m\n" "$*"; }
pyx() { python -c "import sys,json; $1"; }

hdr "1. Login CUSTOMER"
TOK=$(curl -fsS -X POST "$GW/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"an.nguyen@example.com","password":"Demo@123"}' \
  | pyx "print(json.load(sys.stdin)['accessToken'])")
green "token OK"

hdr "2. Lấy 2 product ID + branch HCM ID"
P1=$(curl -fsS "$GW/api/products?size=2" | pyx "d=json.load(sys.stdin); print(d['content'][0]['id'])")
P2=$(curl -fsS "$GW/api/products?size=2" | pyx "d=json.load(sys.stdin); print(d['content'][1]['id'])")
BR=$(curl -fsS "$GW/api/branches" | pyx "d=json.load(sys.stdin); print([b['id'] for b in d if (b['code'] or '').upper()=='HCM'][0])")
echo "  p1=$P1"; echo "  p2=$P2"; echo "  branch=$BR"

hdr "3. Sync cart từ guest (2 item)"
curl -fsS -X POST "$GW/api/cart/sync" -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' \
  -d "{\"items\":[{\"productId\":\"$P1\",\"quantity\":1},{\"productId\":\"$P2\",\"quantity\":2}]}" \
  | pyx "d=json.load(sys.stdin); print(f'  items={len(d[\"items\"])} subtotal={d[\"subtotal\"]}')"

hdr "4. GET cart (enriched)"
curl -fsS "$GW/api/cart" -H "Authorization: Bearer $TOK" \
  | pyx "d=json.load(sys.stdin); [print(f'  - {x[\"tenSp\"]} x{x[\"quantity\"]} = {x[\"giaHienTai\"]}') for x in d['items']]; print(f'  subtotal={d[\"subtotal\"]}')"

hdr "5. Set quantity item 1 = 3"
curl -fsS -X PUT "$GW/api/cart/items/$P1" -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' \
  -d '{"quantity":3}' \
  | pyx "d=json.load(sys.stdin); print(f'  subtotal={d[\"subtotal\"]}')"

hdr "6. Checkout STORE_PICKUP @HCM với voucher WELCOME10"
RESP=$(curl -fsS -X POST "$GW/api/orders/checkout" -H "Authorization: Bearer $TOK" -H 'Content-Type: application/json' \
  -d "{\"deliveryType\":\"STORE_PICKUP\",\"branchId\":\"$BR\",\"voucherCode\":\"WELCOME10\",\"paymentMethod\":\"COD\"}")
echo "$RESP" | pyx "d=json.load(sys.stdin); print(f'  maDh={d[\"maDh\"]} status={d[\"currentStatus\"]} subtotal={d[\"subtotal\"]} discount={d[\"discountAmount\"]} vat={d[\"vatAmount\"]} total={d[\"total\"]}')"
MADH=$(echo "$RESP" | pyx "print(json.load(sys.stdin)['maDh'])")

hdr "7. Order detail + event timeline"
curl -fsS "$GW/api/orders/me/$MADH" -H "Authorization: Bearer $TOK" \
  | pyx "d=json.load(sys.stdin); print(f'  status={d[\"order\"][\"currentStatus\"]}'); [print(f'  - seq={e[\"seq\"]} {e[\"type\"]}') for e in d['events']]"

hdr "8. Cart đã clear sau checkout?"
curl -fsS "$GW/api/cart" -H "Authorization: Bearer $TOK" \
  | pyx "d=json.load(sys.stdin); print(f'  items={len(d[\"items\"])} (kỳ vọng 0)')"

hdr "9. Inventory đã reserve?"
docker exec thluxury-postgres psql -U postgres -d thluxury -c "SELECT product_id, branch_id, quantity, reserved_quantity FROM inventory.inventory WHERE reserved_quantity > 0 LIMIT 5;" 2>/dev/null || true

hdr "10. RabbitMQ queues + order.events exchange"
docker exec thluxury-rabbitmq rabbitmqctl --quiet list_queues name messages consumers 2>/dev/null | grep -E "inventory|^name" || true
docker exec thluxury-rabbitmq rabbitmqctl --quiet list_exchanges name type 2>/dev/null | grep "order.events" || true

hdr "11. List my orders"
curl -fsS "$GW/api/orders/me" -H "Authorization: Bearer $TOK" \
  | pyx "d=json.load(sys.stdin); print(f'  total={d[\"total\"]}'); [print(f'  - {o[\"maDh\"]} {o[\"currentStatus\"]} {o[\"total\"]}') for o in d['content'][:5]]"

hdr "12. Voucher CUSTOMER validate"
curl -fsS "$GW/api/vouchers/SUMMER200K/validate" -H "Authorization: Bearer $TOK" \
  | pyx "d=json.load(sys.stdin); print(f'  {d[\"code\"]} type={d[\"type\"]} value={d[\"value\"]} min={d[\"minOrderValue\"]}')"

green "Smoke test S4 done"
