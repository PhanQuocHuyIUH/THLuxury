'use client';
import { apiClient } from '@/lib/api';
import { useAuthStore } from '@/store/auth';
import { useCartStore } from '@/store/cart';
import type { AuthUser } from '@/types';

export interface LoginResult {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
}

/**
 * Lưu session vào store rồi best-effort sync giỏ hàng local lên server.
 * Dùng chung cho đăng nhập email/password và Google OAuth.
 */
export async function applySession(data: LoginResult): Promise<void> {
  useAuthStore.getState().setSession(data.accessToken, data.refreshToken, data.user);

  try {
    const items = useCartStore.getState().items.map((i) => ({
      productId: i.productId,
      quantity: i.quantity
    }));
    if (items.length > 0) {
      await apiClient.post('/api/cart/sync', { items });
    }
  } catch {
    // ignore — sync giỏ hàng là best-effort, không chặn luồng đăng nhập
  }
}
