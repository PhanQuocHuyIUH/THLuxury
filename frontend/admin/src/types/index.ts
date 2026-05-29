export type Role = 'CUSTOMER' | 'BRANCH_MANAGER' | 'ADMIN';

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  role: Role;
  branchId?: string | null;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

export interface Voucher {
  id: string;
  code: string;
  type: 'PERCENT' | 'FIXED';
  value: number;
  minOrderValue: number;
  maxDiscount?: number | null;
  expiresAt?: string | null;
  usageLimit?: number | null;
  usedCount: number;
  enabled: boolean;
}

export interface CreateVoucherRequest {
  code: string;
  type: 'PERCENT' | 'FIXED';
  value: number;
  minOrderValue?: number;
  maxDiscount?: number | null;
  expiresAt?: string | null;
  usageLimit?: number | null;
  enabled?: boolean;
}

export interface OrderSummary {
  id: string;
  maDh: string;
  currentStatus: string;
  subtotal: number;
  discountAmount: number;
  vatAmount: number;
  total: number;
  voucherCode?: string | null;
  deliveryType?: string | null;
  branchId: string;
  createdAt: string;
  nextStatuses?: string[];
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  total: number;
}

export interface InventoryRow {
  id: string;
  productId: string;
  branchId: string;
  quantity: number;
  reservedQuantity: number;
}

export interface InventoryMovement {
  id: string;
  productId: string;
  branchId: string;
  type: string;
  quantity: number;
  reference?: string | null;
  createdAt: string;
}

export interface Branch {
  id: string;
  code: string;
  name: string;
  address?: string | null;
  city?: string | null;
  district?: string | null;
  ward?: string | null;
  phone?: string | null;
  lat?: number | null;
  lng?: number | null;
  enabled: boolean;
}

export interface CreateBranchRequest {
  code: string;
  name: string;
  address?: string;
  city?: string;
  district?: string;
  ward?: string;
  phone?: string;
  lat?: number | null;
  lng?: number | null;
}

export interface UpdateBranchRequest {
  name?: string;
  address?: string;
  city?: string;
  district?: string;
  ward?: string;
  phone?: string;
  lat?: number | null;
  lng?: number | null;
  enabled?: boolean;
}

export interface UserView {
  id: string;
  email: string;
  fullName: string;
  phone?: string | null;
  role: Role;
  branchId?: string | null;
  enabled: boolean;
  createdAt: string;
}

export interface CreateBranchManagerRequest {
  email: string;
  password: string;
  fullName: string;
  phone?: string;
  branchId: string;
}

export interface UpdateUserRequest {
  fullName?: string;
  phone?: string;
  role?: Role;
  branchId?: string | null;
  enabled?: boolean;
}

export interface ProductSummary {
  id: string;
  maSp: number;
  tenSp: string;
  loaiSp: string;
  giaBanDau: number;
  giaGiamGia: number;
  giaHienTai: number;
  thumbnail?: string | null;
  status: string;
}

export interface ProductImageRequest {
  imageUrl: string;
  sortOrder?: number;
}

export interface CreateProductRequest {
  tenSp: string;
  loaiSp: string;
  giaBanDau: number;
  giaGiamGia?: number;
  trongLuong?: number;
  hamLuong?: string;
  loaiDa?: string;
  mauDa?: string;
  gioiTinh?: string;
  thuongHieu?: string;
  description?: string;
  images?: ProductImageRequest[];
}
