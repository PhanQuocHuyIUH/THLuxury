import { apiClient } from '@/lib/api';
import type { OrderSummary, PagedResponse } from '@/types';

export interface ListOrdersParams {
  statuses?: string[];
  branchId?: string;
  page?: number;
  size?: number;
}

export async function listManagedOrders(
  params: ListOrdersParams = {}
): Promise<PagedResponse<OrderSummary>> {
  const query = new URLSearchParams();
  if (params.statuses && params.statuses.length > 0) {
    params.statuses.forEach(s => query.append('statuses', s));
  }
  if (params.branchId) query.append('branchId', params.branchId);
  query.append('page', String(params.page ?? 0));
  query.append('size', String(params.size ?? 20));

  const { data } = await apiClient.get<PagedResponse<OrderSummary>>(`/api/orders/manage?${query.toString()}`);
  return data;
}

export async function getOrderDetail(maDh: string): Promise<any> {
  const { data } = await apiClient.get(`/api/orders/manage/${maDh}`);
  return data;
}

export async function updateOrderStatus(
  maDh: string,
  targetStatus: string
): Promise<OrderSummary> {
  const { data } = await apiClient.patch<OrderSummary>(
    `/api/orders/manage/${maDh}/status`,
    { targetStatus }
  );
  return data;
}

export const ORDER_STATUSES = [
  'CREATED',
  'RESERVED',
  'PRICED',
  'PAID',
  'CONFIRMED',
  'PREPARING',
  'READY_FOR_PICKUP',
  'SHIPPING',
  'DELIVERED',
  'COMPLETED',
  'CANCELLED',
  'FAILED'
] as const;

export type OrderStatusValue = (typeof ORDER_STATUSES)[number];

const STATUS_LABEL: Record<string, { label: string; bg: string }> = {
  CREATED: { label: 'Mới tạo', bg: 'secondary' },
  RESERVED: { label: 'Đã giữ kho', bg: 'info' },
  PRICED: { label: 'Đã định giá', bg: 'info' },
  PAID: { label: 'Đã thanh toán', bg: 'success' },
  CONFIRMED: { label: 'Đã xác nhận (COD)', bg: 'warning' },
  PREPARING: { label: 'Đang chuẩn bị', bg: 'primary' },
  READY_FOR_PICKUP: { label: 'Sẵn sàng nhận', bg: 'primary' },
  SHIPPING: { label: 'Đang giao', bg: 'primary' },
  DELIVERED: { label: 'Đã giao', bg: 'success' },
  COMPLETED: { label: 'Hoàn tất', bg: 'success' },
  CANCELLED: { label: 'Đã huỷ', bg: 'dark' },
  FAILED: { label: 'Thất bại', bg: 'danger' }
};

export function statusBadge(s: string): { label: string; bg: string } {
  return STATUS_LABEL[s] ?? { label: s, bg: 'secondary' };
}
