import { apiClient } from '@/lib/api';
import type {
  CreateBranchManagerRequest,
  PagedResponse,
  Role,
  UpdateUserRequest,
  UserView
} from '@/types';

export interface ListUsersParams {
  role?: Role;
  branchId?: string;
  page?: number;
  size?: number;
}

export async function listUsers(
  params: ListUsersParams = {}
): Promise<PagedResponse<UserView>> {
  const { data } = await apiClient.get('/api/admin/users', {
    params: {
      role: params.role || undefined,
      branchId: params.branchId || undefined,
      page: params.page ?? 0,
      size: params.size ?? 20
    }
  });
  const content = data.content as UserView[];
  return {
    content,
    page: data.number ?? data.page ?? 0,
    size: data.size ?? 20,
    total: data.totalElements ?? data.total ?? content.length
  };
}

export async function createBranchManager(
  payload: CreateBranchManagerRequest
): Promise<UserView> {
  const { data } = await apiClient.post<UserView>(
    '/api/admin/users/branch-managers',
    payload
  );
  return data;
}

export async function updateUser(
  id: string,
  payload: UpdateUserRequest
): Promise<UserView> {
  const { data } = await apiClient.put<UserView>(`/api/admin/users/${id}`, payload);
  return data;
}
