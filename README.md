# THLuxury — Microservices E-Commerce Platform

> Hệ thống thương mại điện tử trang sức cao cấp, kiến trúc microservices (8 services + 2 frontend). Dự án tập trung vào việc triển khai một hệ thống mở rộng, chịu tải tốt với đầy đủ các tính năng của một sàn TMĐT hiện đại.

## 🏗️ Kiến trúc hệ thống

Dự án được xây dựng dựa trên mô hình Microservices với các công nghệ chính:
- **Backend**: Java Spring Boot (Microservices), Python FastAPI (AI Service).
- **Frontend**: Next.js 14 (Storefront), React/Vite (Admin Dashboard).
- **Giao tiếp**: REST API, gRPC (Internal communication), RabbitMQ (Event-driven).
- **Hạ tầng**: Docker Compose, Postgres, Redis, MailHog.
- **Giám sát (Observability)**: Prometheus, Grafana, Loki, Promtail.

## 🧱 Cấu trúc thư mục

```
THLuxury/
├── infra/                # Docker-compose & cấu hình hạ tầng (Database, Message Broker, v.v.)
├── services/             # 8 microservices backend
│   ├── api-gateway/       (Spring Cloud Gateway)
│   ├── identity-service/  (Spring Boot - Auth & User)
│   ├── catalog-service/   (Spring Boot + CQRS - Sản phẩm & Danh mục)
│   ├── inventory-service/ (Spring Boot + gRPC - Kho hàng)
│   ├── order-service/     (Spring Boot + Saga + Event Sourcing - Đơn hàng)
│   ├── payment-service/   (Spring Boot + gRPC - Thanh toán)
│   ├── notification-service/ (Spring Boot + RabbitMQ - Thông báo)
│   └── ai-service/        (Python FastAPI - Chatbot thông minh)
├── frontend/
│   ├── storefront/        (Next.js 14 — Giao diện cho khách hàng)
│   └── admin/             (Vite + React — Giao diện quản trị & chi nhánh)
├── scripts/              # Tiện ích quản lý (Seed dữ liệu, Healthcheck, Demo script)
└── .github/workflows/    # Cấu hình CI/CD
```

*Lưu ý: Thư mục `docs/` (tài liệu chi tiết) và `5TLuxury/` (dự án mẫu tham chiếu) đã được đưa vào `.gitignore` để bảo mật và tối ưu repository.*

## 🚀 Quickstart

```powershell
# 1. Khởi tạo cấu hình môi trường
Copy-Item infra/.env.template infra/.env

# 2. Bật hạ tầng core (postgres + redis + rabbit + mailhog)
docker compose --env-file infra/.env -f infra/docker-compose.yml up -d

# 3. Bật toàn bộ stack bao gồm cả hệ thống giám sát
docker compose --env-file infra/.env `
  -f infra/docker-compose.yml `
  -f infra/docker-compose.observability.yml up -d

# 4. Kiểm tra trạng thái hoạt động của các service
./scripts/healthcheck.sh
```

## 🌐 URL hữu ích khi hệ thống đang chạy

| Dịch vụ | URL | Thông tin đăng nhập |
|---|---|---|
| **Storefront** | http://localhost:3000 | — |
| **Admin Dashboard** | http://localhost:5173 | (Dùng tài khoản seed mặc định) |
| **API Gateway** | http://localhost:8080 | — |
| **RabbitMQ UI** | http://localhost:15672 | `admin` / `admin` |
| **MailHog UI** | http://localhost:8025 | — |
| **Prometheus** | http://localhost:9090 | — |
| **Grafana** | http://localhost:3001 | `admin` / `admin` |

## 📌 Trạng thái dự án

Hệ thống đã hoàn thiện toàn bộ các giai đoạn phát triển chính:
- ✅ **Backend**: Hoàn tất 8 microservices với đầy đủ logic nghiệp vụ, gRPC communication và Saga Pattern.
- ✅ **Frontend**: Hoàn thiện Storefront (Next.js) và Admin Dashboard (React).
- ✅ **AI Integration**: Tích hợp chatbot hỗ trợ khách hàng dựa trên FastAPI.
- ✅ **Observability**: Thiết lập đầy đủ Dashboard giám sát trên Grafana.
- ✅ **Hardening**: Áp dụng Rate-limiting, Circuit Breaker và tối ưu hóa hệ thống.

## 🧪 Demo & Kiểm thử

Để trải nghiệm toàn bộ luồng nghiệp vụ tự động (từ đăng ký, mua hàng đến kiểm tra thông báo và chat AI):

```powershell
pwsh ./scripts/demo.ps1
```

- **Hệ thống giám sát**: Truy cập Grafana (http://localhost:3001) để xem các thông số về Business KPIs, RabbitMQ throughput và tình trạng sức khỏe microservices.
- **API Testing**: Có thể sử dụng Postman collection để test trực tiếp các endpoint thông qua API Gateway tại cổng `8080`.
