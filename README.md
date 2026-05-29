# THLuxury — Microservices E-Commerce Platform

> Hệ thống thương mại điện tử trang sức cao cấp, kiến trúc microservices (8 services + 2 frontend), kế thừa UI/data mẫu từ project `5TLuxury` (MERN).

## 🏛️ Tài liệu kiến trúc

| File | Nội dung |
|---|---|
| [`docs/Đề bài.txt`](docs/Đề%20bài.txt) | Đề bài gốc + scope kiểm tra |
| [`docs/plan.md`](docs/plan.md) | Plan tổng quan ban đầu |
| [`docs/implementation-plan.md`](docs/implementation-plan.md) | **Plan chi tiết, implement-ready** (ADR + schema + endpoint spec + roadmap) |

## 🧱 Cấu trúc thư mục

```
THLuxury/
├── 5TLuxury/             # project MERN gốc (giữ để tham chiếu UI/data)
├── docs/                 # tài liệu
├── infra/                # docker-compose + config Postgres/Redis/Rabbit/Mail/Observability
│                         # (gRPC .proto nằm trong từng service: services/*/src/main/proto)
├── services/             # 8 microservices backend
│   ├── api-gateway/       (Spring Cloud Gateway)
│   ├── identity-service/  (Spring Boot)
│   ├── catalog-service/   (Spring Boot + CQRS)
│   ├── inventory-service/ (Spring Boot + gRPC)
│   ├── order-service/     (Spring Boot + Saga + Event Sourcing)
│   ├── payment-service/   (Spring Boot + gRPC)
│   ├── notification-service/ (Spring Boot + RabbitMQ consumer)
│   └── ai-service/        (Python FastAPI)
├── frontend/
│   ├── storefront/        (Next.js 14 — GUEST + CUSTOMER)
│   └── admin/             (Vite + React — ADMIN + BRANCH_MANAGER)
├── scripts/              # tiện ích (copy ảnh, seed, healthcheck)
└── .github/workflows/    # CI
```

## 🚀 Quickstart

```powershell
# 1. Copy env mẫu
Copy-Item infra/.env.template infra/.env

# 2. (Tuỳ chọn) copy 109 ảnh sản phẩm từ 5TLuxury sang storefront/public
./scripts/copy-images.ps1

# 3. Bật hạ tầng core (postgres + redis + rabbit + mailhog)
docker compose --env-file infra/.env -f infra/docker-compose.yml up -d

# 3b. Bật cả observability (prometheus + grafana + loki)
docker compose --env-file infra/.env `
  -f infra/docker-compose.yml `
  -f infra/docker-compose.observability.yml up -d

# 4. Healthcheck
./scripts/healthcheck.sh

# 5. Tắt
docker compose -f infra/docker-compose.yml down
```

## 🌐 URL hữu ích khi stack đang chạy

| Dịch vụ | URL | Credentials |
|---|---|---|
| Storefront (Next.js) | http://localhost:3000 | — |
| Admin Dashboard | http://localhost:5173 | seed accounts (xem implementation-plan §6.5) |
| API Gateway | http://localhost:8080 | — |
| RabbitMQ Management | http://localhost:15672 | admin / admin |
| MailHog UI | http://localhost:8025 | — |
| Prometheus | http://localhost:9090 | — |
| Grafana | http://localhost:3001 | admin / admin |
| Postgres | localhost:5432 | postgres / postgres |

## 📌 Trạng thái triển khai

| Sprint | Phạm vi | Trạng thái |
|---|---|---|
| S0 | Skeleton + hạ tầng docker-compose | ✅ done |
| S1 | Identity Service + API Gateway | ✅ done |
| S2 | Catalog Service (CQRS) | ✅ done |
| S3 | Inventory Service (gRPC) | ✅ done |
| S4 | Order Service (Saga + ES, core) | ✅ done |
| S5 | Payment Service + Saga full | ✅ done |
| S6 | Notification Service | ✅ done |
| S7 | AI Service | ✅ done |
| S8 | Storefront Next.js (+ Google OAuth, quên/đặt lại mật khẩu) | ✅ done |
| S9 | Admin Dashboard | ✅ done |
| S10 | Observability (Prometheus/Grafana/Loki + tracing + 3 dashboard) | ✅ done |
| S11 | Hardening (rate-limit + circuit breaker) + demo prep | ✅ done |

Xem chi tiết DoD từng sprint trong [`docs/implementation-plan.md`](docs/implementation-plan.md#7-roadmap-chi-tiết-30-ngày-làm-việc).

## 🧪 Demo & kiểm thử API

```powershell
# Kịch bản demo end-to-end (đăng ký → mua hàng → chat AI), < 10 phút
pwsh ./scripts/demo.ps1
```

- **Postman**: import [`docs/THLuxury.postman_collection.json`](docs/THLuxury.postman_collection.json) — chạy `Auth > Login` trước, token tự lưu vào biến.
- **Kiến trúc C4** (cho slide): [`docs/architecture-c4.md`](docs/architecture-c4.md).
- **Grafana dashboards** (provisioned sẵn): *Microservices Overview*, *RabbitMQ*, *Business KPIs* tại http://localhost:3001.
