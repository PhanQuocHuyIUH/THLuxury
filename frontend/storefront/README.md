# THLuxury Storefront

Next.js 14 (App Router) storefront cho THLuxury — port từ 5TLuxury UI sang TSX, gọi qua API Gateway.

## Yêu cầu

- Node.js 20+
- API Gateway đang chạy ở `http://localhost:8080`

## Phát triển local

```bash
cd frontend/storefront
cp .env.local.example .env.local
npm install --legacy-peer-deps
npm run dev
```

Mở http://localhost:3000.

## Build production

```bash
npm run build
npm run start
```

## Docker

```bash
docker compose -f infra/docker-compose.yml up storefront --build
```

## Cấu trúc

- `app/` — Routes (App Router):
  - `/` — Home, ISR 1h
  - `/products` — Danh sách + lọc, SSR
  - `/products/[id]` — Chi tiết sản phẩm, ISR 1 ngày
  - `/cart` — Giỏ hàng (localStorage qua Zustand)
  - `/checkout` — Multi-step checkout (Address → Review → Payment)
  - `/order/[maDh]` — Trang xác nhận / theo dõi đơn
  - `/login`, `/register`, `/profile`
  - `/search`, `/contact`, `/information`
- `components/` — Header, Footer, ChatbotModal, ChatbotLauncher, ProductCard, ListProduct, Loading, ErrorBox
- `lib/` — `api.ts` (axios + refresh interceptor), `products.ts` (server fetch helpers), `format.ts`
- `store/` — Zustand stores: `auth`, `cart` (persist localStorage)
- `types/` — Shared TypeScript interfaces
- `public/products/` — 95 ảnh sản phẩm copy từ 5TLuxury
- `public/assets/` — Banner, danh mục, video hero

## API contract

Mọi request đi qua API Gateway (`NEXT_PUBLIC_API_URL`). Refresh token tự động qua axios interceptor.

| FE Path | Backend |
|---|---|
| Product list/detail | Catalog Service |
| Cart sync | Order Service |
| Checkout | Order Saga |
| Branches | Identity Service (`/api/branches`) |
| Chatbot | AI Service (`/api/ai/chat`) |
| Auth | Identity Service |

## Notes

- Auth dùng localStorage (Zustand persist). Refresh token cũng nằm trong store — đơn giản hoá cho phase demo. Nếu cần bảo mật hơn, port sang Next API route + httpOnly cookie.
- Cart cho GUEST nằm trong localStorage; khi login sẽ tự sync lên server (`POST /api/cart/sync`).
- Hình ảnh sản phẩm trong DB seed dùng URL tương đối `/products/{X.Y}.png` — được Next.js serve từ `public/products/`.
