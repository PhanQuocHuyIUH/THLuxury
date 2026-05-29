export interface Product {
  id: string;
  maSp: number;
  tenSp: string;
  loaiSp: string;
  giaBanDau: number;
  giaGiamGia: number;
  giaHienTai: number;
  trongLuong?: number;
  hamLuong?: string;
  loaiDa?: string;
  mauDa?: string;
  gioiTinh?: string;
  thuongHieu?: string;
  description?: string;
  status: string;
  images: string[];
}

export interface ProductListResponse {
  content: Product[];
  page: number;
  size: number;
  total: number;
}

export interface CartItem {
  productId: string;
  quantity: number;
  tenSp?: string;
  hinh?: string;
  giaHienTai: number;
}

export interface CartView {
  items: CartItem[];
  subtotal: number;
}

export interface AuthUser {
  id: string;
  email: string;
  fullName: string;
  phone?: string;
  role: 'CUSTOMER' | 'BRANCH_MANAGER' | 'ADMIN';
  branchId?: string | null;
}

export interface TokenPair {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

export interface Branch {
  id: string;
  code: string;
  name: string;
  address?: string;
  city?: string;
  district?: string;
  ward?: string;
  phone?: string;
}

export interface OrderItem {
  productId: string;
  maSp?: number;
  tenSp: string;
  hinh?: string;
  soLuong: number;
  giaMua: number;
}

export interface OrderSummary {
  id: string;
  maDh: string;
  currentStatus: string;
  subtotal: number;
  discountAmount: number;
  vatAmount: number;
  total: number;
  voucherCode?: string;
  deliveryType: string;
  branchId: string;
  createdAt: string;
}

export interface OrderDetail extends OrderSummary {
  customerSnapshot?: any;
  addressSnapshot?: any;
  itemsSnapshot?: OrderItem[];
  paymentMethod?: string;
  failureReason?: string;
}

export interface SuggestedProduct {
  productId: string;
  tenSP: string;
  giaSP: number;
  imageUrl: string;
}

export interface ChatResponse {
  reply: string;
  suggestedProducts?: SuggestedProduct[];
  intent: string;
}
