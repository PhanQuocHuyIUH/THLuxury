#!/usr/bin/env bash
# ============================================================
# THLuxury — Health check toàn bộ container đang chạy
# Sử dụng: bash scripts/healthcheck.sh
# ============================================================
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# Mỗi entry: "label|command-to-run-inside-container|container-name"
checks=(
    "Postgres   | pg_isready -U postgres -d thluxury           | thluxury-postgres"
    "Redis      | redis-cli ping                                | thluxury-redis"
    "RabbitMQ   | rabbitmq-diagnostics -q ping                  | thluxury-rabbitmq"
    "MailHog    | wget -q -O- http://localhost:8025 >/dev/null  | thluxury-mailhog"
)

# Application service health endpoints (sẽ DOWN cho đến S1+)
http_checks=(
    "API Gateway       | http://localhost:8080/actuator/health"
    "Identity Service  | http://localhost:8081/actuator/health"
    "Catalog Service   | http://localhost:8082/actuator/health"
    "Inventory Service | http://localhost:8083/actuator/health"
    "Order Service     | http://localhost:8084/actuator/health"
    "Payment Service   | http://localhost:8085/actuator/health"
    "Notification Svc  | http://localhost:8086/actuator/health"
    "AI Service        | http://localhost:8087/health"
)

OK="\033[32m✓\033[0m"
FAIL="\033[31m✗\033[0m"
SKIP="\033[33m-\033[0m"

echo "──── Container healthchecks ─────────────────────────────"
for row in "${checks[@]}"; do
    IFS='|' read -r label cmd container <<<"$row"
    label=$(echo "$label" | xargs); cmd=$(echo "$cmd" | xargs); container=$(echo "$container" | xargs)
    if ! docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        printf "  ${SKIP} %-12s (container not running)\n" "$label"
        continue
    fi
    if docker exec "$container" sh -c "$cmd" >/dev/null 2>&1; then
        printf "  ${OK} %-12s\n" "$label"
    else
        printf "  ${FAIL} %-12s\n" "$label"
    fi
done

echo ""
echo "──── Application services (sẽ DOWN cho đến S1+) ─────────"
for row in "${http_checks[@]}"; do
    IFS='|' read -r label url <<<"$row"
    label=$(echo "$label" | xargs); url=$(echo "$url" | xargs)
    if curl -fsS -o /dev/null --max-time 2 "$url"; then
        printf "  ${OK} %-18s %s\n" "$label" "$url"
    else
        printf "  ${SKIP} %-18s %s\n" "$label" "$url"
    fi
done
