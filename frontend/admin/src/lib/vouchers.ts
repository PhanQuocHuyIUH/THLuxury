import { apiClient } from '@/lib/api';
import type { CreateVoucherRequest, Voucher } from '@/types';

export async function listVouchers(): Promise<Voucher[]> {
  const { data } = await apiClient.get<Voucher[]>('/api/vouchers');
  return data;
}

export async function createVoucher(payload: CreateVoucherRequest): Promise<Voucher> {
  const { data } = await apiClient.post<Voucher>('/api/vouchers', payload);
  return data;
}

export async function disableVoucher(id: string): Promise<void> {
  await apiClient.delete(`/api/vouchers/${id}`);
}
