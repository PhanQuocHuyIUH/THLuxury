#!/usr/bin/env bash
# Smoke test for Sprint 2 — Catalog Service via Gateway.
set -euo pipefail
GW="${GATEWAY:-http://localhost:8080}"

green() { printf "\033[32m✓\033[0m %s\n" "$*"; }
red()   { printf "\033[31m✗\033[0m %s\n" "$*"; }
hdr()   { printf "\n\033[36m── %s ──\033[0m\n" "$*"; }
jq_or_py() { python -c "import sys,json; $1" ; }

hdr "1. List page 0 size 5 (cache miss → set)"
LIST1=$(curl -fsS "$GW/api/products?size=5")
echo "$LIST1" | jq_or_py "d=json.load(sys.stdin); print(f'  total={d[\"total\"]} returned={len(d[\"content\"])}'); print(f'  first: maSp={d[\"content\"][0][\"maSp\"]} tenSp={d[\"content\"][0][\"tenSp\"]} gia={d[\"content\"][0][\"giaHienTai\"]}')"

hdr "2. Redis cache keys"
docker exec thluxury-redis redis-cli --raw KEYS 'cat:*'

hdr "3. List lần 2 → kiểm log cho 'cache HIT'"
curl -fsS "$GW/api/products?size=5" > /dev/null
sleep 1
docker logs thluxury-catalog 2>&1 | grep -E "cache (HIT|MISS)" | tail -3 || true

hdr "4. Filter loaiSp=Nhẫn (URL pre-encoded vì Git Bash hỏng UTF-8 args)"
curl -fsS "$GW/api/products?loaiSp=Nh%E1%BA%ABn&size=3" \
  | jq_or_py "d=json.load(sys.stdin); print(f'  total={d[\"total\"]}'); [print(f'  - {x[\"maSp\"]}: {x[\"tenSp\"]} ({x[\"loaiSp\"]})') for x in d['content']]"

hdr "5. Search keyword 'kim cương'"
curl -fsS "$GW/api/products?keyword=kim%20c%C6%B0%C6%A1ng&size=3" \
  | jq_or_py "d=json.load(sys.stdin); print(f'  total={d[\"total\"]}'); [print(f'  - {x[\"maSp\"]}: {x[\"tenSp\"]}') for x in d['content']]"

hdr "6. Detail by maSp=1"
curl -fsS "$GW/api/products/by-ma-sp/1" \
  | jq_or_py "d=json.load(sys.stdin); print(f'  tenSp={d[\"tenSp\"]} giaHienTai={d[\"giaHienTai\"]}'); print(f'  images={d[\"images\"]}')"

hdr "7. Categories"
curl -fsS "$GW/api/categories" | jq_or_py "print('  ' + ', '.join(json.load(sys.stdin)))"

hdr "8. POST product (CUSTOMER → 403)"
USER_TOKEN=$(curl -fsS -X POST "$GW/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"an.nguyen@example.com","password":"Demo@123"}' \
  | jq_or_py "print(json.load(sys.stdin)['accessToken'])")
code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$GW/api/products" \
  -H "Authorization: Bearer $USER_TOKEN" -H 'Content-Type: application/json' --data-binary @"$(dirname "$0")/fixtures/new-product.json")
echo "  HTTP $code"

hdr "9. POST product (ADMIN → 201)"
ADMIN_TOKEN=$(curl -fsS -X POST "$GW/api/auth/login" -H 'Content-Type: application/json' \
  -d '{"email":"admin@thluxury.local","password":"Demo@123"}' \
  | jq_or_py "print(json.load(sys.stdin)['accessToken'])")
NEW=$(curl -fsS -X POST "$GW/api/products" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json; charset=utf-8' \
  --data-binary @"$(dirname "$0")/fixtures/new-product.json")
echo "  $NEW"
NEW_ID=$(echo "$NEW" | jq_or_py "print(json.load(sys.stdin)['id'])")

hdr "10. Đợi event consumer rebuild projection"
for i in 1 2 3 4 5 6 7 8; do
  HTTP=$(curl -s -o /tmp/detail.json -w "%{http_code}" "$GW/api/products/$NEW_ID")
  if [ "$HTTP" = "200" ]; then
    green "Sau ${i}s: detail readable"
    jq_or_py "d=json.load(sys.stdin); print(f'  - {d[\"tenSp\"]} maSp={d[\"maSp\"]} gia={d[\"giaHienTai\"]}')" < /tmp/detail.json
    break
  fi
  sleep 1
done

hdr "11. Search lại 'cẩm thạch' (cache đã invalidate)"
curl -fsS "$GW/api/products?keyword=c%E1%BA%A9m%20th%E1%BA%A1ch&size=5" \
  | jq_or_py "d=json.load(sys.stdin); print(f'  total={d[\"total\"]}'); [print(f'  - {x[\"tenSp\"]}') for x in d['content']]"

hdr "12. RabbitMQ queue stats"
docker exec thluxury-rabbitmq rabbitmqctl --quiet list_queues name messages consumers 2>/dev/null | grep -E "catalog|^name" || true

green "Smoke test S2 done"
