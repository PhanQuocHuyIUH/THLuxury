import { apiClient } from '@/lib/api';
import type { Branch, CreateBranchRequest, UpdateBranchRequest } from '@/types';

export async function listBranches(): Promise<Branch[]> {
  const { data } = await apiClient.get<Branch[]>('/api/branches');
  return data;
}

export async function createBranch(payload: CreateBranchRequest): Promise<Branch> {
  const { data } = await apiClient.post<Branch>('/api/branches', payload);
  return data;
}

export async function updateBranch(
  id: string,
  payload: UpdateBranchRequest
): Promise<Branch> {
  const { data } = await apiClient.put<Branch>(`/api/branches/${id}`, payload);
  return data;
}
