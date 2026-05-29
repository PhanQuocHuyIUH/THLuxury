import { apiClient } from '@/lib/api';
import type { CreateProductRequest, PagedResponse, ProductSummary } from '@/types';

export interface ListProductsParams {
  keyword?: string;
  loaiSp?: string;
  page?: number;
  size?: number;
}

export async function listProducts(
  params: ListProductsParams = {}
): Promise<PagedResponse<ProductSummary>> {
  const { data } = await apiClient.get('/api/products', {
    params: {
      keyword: params.keyword || undefined,
      loaiSp: params.loaiSp || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20
    }
  });
  return data as PagedResponse<ProductSummary>;
}

export async function createProduct(payload: CreateProductRequest): Promise<string> {
  const { data } = await apiClient.post<{ id: string }>('/api/products', payload);
  return data.id;
}

export async function archiveProduct(id: string): Promise<void> {
  await apiClient.delete(`/api/products/${id}`);
}

export async function listCategories(): Promise<string[]> {
  const { data } = await apiClient.get<string[]>('/api/products/categories');
  return data;
}
