import { apiClient } from '@/lib/api';
import type { InventoryMovement, InventoryRow } from '@/types';

export async function listInventory(branchId?: string): Promise<InventoryRow[]> {
  const { data } = await apiClient.get<InventoryRow[]>('/api/inventory', {
    params: { branchId: branchId || undefined }
  });
  return data;
}

export async function listLowStock(
  branchId?: string,
  threshold?: number
): Promise<InventoryRow[]> {
  const { data } = await apiClient.get<InventoryRow[]>('/api/inventory/low-stock', {
    params: {
      branchId: branchId || undefined,
      threshold: threshold ?? undefined
    }
  });
  return data;
}

export interface StockInPayload {
  productId: string;
  branchId: string;
  quantity: number;
  reference?: string;
}

export async function stockIn(payload: StockInPayload): Promise<void> {
  await apiClient.post('/api/inventory/stock-in', payload);
}

export async function listMovements(
  productId: string,
  branchId?: string
): Promise<InventoryMovement[]> {
  const { data } = await apiClient.get<InventoryMovement[]>('/api/inventory/movements', {
    params: { productId, branchId: branchId || undefined }
  });
  return data;
}
