# THLuxury Admin

Vite + React + TS admin console (Sprint 9).

## Dev

```powershell
cd frontend/admin
cp .env.example .env
npm install
npm run dev   # http://localhost:5173
```

Đăng nhập bằng tài khoản role `ADMIN` hoặc `BRANCH_MANAGER` (Identity Service tại `VITE_API_URL`).

## Hiện trạng

- Auth: JWT login + refresh interceptor (chia sẻ pattern với storefront).
- Router: protected routes, role-based sidebar.
  - **ADMIN**: tất cả module.
  - **BRANCH_MANAGER**: Đơn hàng / Tồn kho / Voucher / Stats — BE tự lọc theo `branchId` trong JWT.
  - **CUSTOMER**: bị chặn ở route gốc.
- Modules:
  - **Dashboard** — hello + role/branch info
  - **Đơn hàng** (`/orders`) — list `/api/orders/manage`, filter trạng thái + chi nhánh (admin), phân trang
  - **Sản phẩm** (`/products`, ADMIN-only) — list `/api/products`, tạo mới, lưu trữ (archive)
  - **Tồn kho** (`/inventory`) — list `/api/inventory`, lọc low-stock, nhập kho qua `/api/inventory/stock-in`
  - **Vouchers** (`/vouchers`) — list/create/disable `/api/vouchers`
  - **Người dùng** (`/users`, ADMIN-only) — list `/api/admin/users`, tạo Branch Manager, khoá/mở khoá
  - **Chi nhánh** (`/branches`, ADMIN-only) — list `/api/branches`, tạo + bật/tắt
  - **Thống kê** (`/stats`) — aggregate client-side trên 200 đơn gần nhất (BE chưa có endpoint stats riêng)

## Pattern thêm module mới

Theo khuôn Vouchers:
1. Thêm type vào `src/types/index.ts`.
2. Tạo API helper tại `src/lib/<resource>.ts`.
3. Tạo page tại `src/pages/<Resource>.tsx` — `useAuthStore(s => s.user?.role)` để gate write actions.
4. Wire vào `src/App.tsx` routes + cập nhật `src/lib/nav.ts` nếu cần.
