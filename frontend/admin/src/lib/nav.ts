import type { Role } from '@/types';

export interface NavItem {
  to: string;
  label: string;
  roles: Role[];
}

export const NAV_ITEMS: NavItem[] = [
  { to: '/', label: 'Dashboard', roles: ['ADMIN', 'BRANCH_MANAGER'] },
  { to: '/orders', label: 'Đơn hàng', roles: ['ADMIN', 'BRANCH_MANAGER'] },
  { to: '/products', label: 'Sản phẩm', roles: ['ADMIN'] },
  { to: '/inventory', label: 'Tồn kho', roles: ['ADMIN', 'BRANCH_MANAGER'] },
  { to: '/vouchers', label: 'Vouchers', roles: ['ADMIN', 'BRANCH_MANAGER'] },
  { to: '/users', label: 'Người dùng', roles: ['ADMIN'] },
  { to: '/branches', label: 'Chi nhánh', roles: ['ADMIN'] },
  { to: '/stats', label: 'Thống kê', roles: ['ADMIN', 'BRANCH_MANAGER'] }
];

export function canAccess(role: Role, path: string): boolean {
  const item = NAV_ITEMS.find((n) => n.to === path);
  return item ? item.roles.includes(role) : true;
}
