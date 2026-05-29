import { API_URL } from '@/lib/api';
import type { Product, ProductListResponse } from '@/types';

// Server-side fetch (Server Components) chạy bên trong container storefront,
// nên không dùng được http://localhost:8080 (trỏ về chính container).
// Trong Docker: INTERNAL_API_URL=http://api-gateway:8080.
// Khi chạy `npm run dev` ở host: biến này trống → fallback về localhost:8080.
const SERVER_API_URL = process.env.INTERNAL_API_URL || API_URL;

interface ServerOpts {
  cache?: RequestCache;
  next?: { revalidate?: number };
}

export async function fetchProducts(
  params: Record<string, string | number | undefined> = {},
  opts: ServerOpts = { next: { revalidate: 60 } }
): Promise<ProductListResponse> {
  const usp = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== '') usp.append(k, String(v));
  }
  const url = `${SERVER_API_URL}/api/products?${usp.toString()}`;
  const res = await fetch(url, { ...opts });
  if (!res.ok) throw new Error(`Failed to fetch products: ${res.status}`);
  const data = await res.json();
  return normalizeListResponse(data);
}

export async function fetchProductById(
  id: string,
  opts: ServerOpts = { next: { revalidate: 60 } }
): Promise<Product | null> {
  const res = await fetch(`${SERVER_API_URL}/api/products/${id}`, opts);
  if (res.status === 404) return null;
  if (!res.ok) throw new Error(`Failed to fetch product: ${res.status}`);
  const data = await res.json();
  return normalizeProduct(data);
}

export async function fetchProductByMaSp(
  maSp: string,
  opts: ServerOpts = { next: { revalidate: 60 } }
): Promise<Product | null> {
  const res = await fetch(`${SERVER_API_URL}/api/products/by-ma-sp/${maSp}`, opts);
  if (res.status === 404) return null;
  if (!res.ok) return null;
  const data = await res.json();
  return normalizeProduct(data);
}

export async function fetchCategories(
  opts: ServerOpts = { next: { revalidate: 600 } }
): Promise<string[]> {
  try {
    const res = await fetch(`${SERVER_API_URL}/api/categories`, opts);
    if (!res.ok) return [];
    const data = await res.json();
    if (Array.isArray(data)) {
      return data.map((d) => (typeof d === 'string' ? d : d.loaiSp || d.name)).filter(Boolean);
    }
    return [];
  } catch {
    return [];
  }
}

function normalizeProduct(raw: any): Product {
  const images = Array.isArray(raw?.images)
    ? raw.images.map((img: any) => (typeof img === 'string' ? img : img?.imageUrl)).filter(Boolean)
    : [];
  return {
    id: String(raw.id ?? raw.productId ?? ''),
    maSp: Number(raw.maSp ?? raw.maSP ?? 0),
    tenSp: raw.tenSp ?? raw.tenSP ?? '',
    loaiSp: raw.loaiSp ?? raw.loaiSP ?? '',
    giaBanDau: Number(raw.giaBanDau ?? 0),
    giaGiamGia: Number(raw.giaGiamGia ?? 0),
    giaHienTai: Number(raw.giaHienTai ?? raw.giaBanDau ?? 0),
    trongLuong: raw.trongLuong != null ? Number(raw.trongLuong) : undefined,
    hamLuong: raw.hamLuong,
    loaiDa: raw.loaiDa,
    mauDa: raw.mauDa,
    gioiTinh: raw.gioiTinh,
    thuongHieu: raw.thuongHieu,
    description: raw.description,
    status: raw.status ?? 'ACTIVE',
    images
  };
}

function normalizeListResponse(data: any): ProductListResponse {
  const items: any[] = Array.isArray(data) ? data : data?.content || [];
  return {
    content: items.map(normalizeProduct),
    page: data?.page ?? 0,
    size: data?.size ?? items.length,
    total: data?.total ?? items.length
  };
}
