import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/auth';
import type { Role } from '@/types';

interface Props {
  children: React.ReactNode;
  allow?: Role[];
}

export default function ProtectedRoute({ children, allow }: Props) {
  const { user, hydrated } = useAuthStore();
  const location = useLocation();

  if (!hydrated) {
    return <div style={{ padding: 24 }}>Đang tải...</div>;
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (user.role === 'CUSTOMER') {
    return <div style={{ padding: 24 }}>Tài khoản không có quyền truy cập admin.</div>;
  }

  if (allow && !allow.includes(user.role)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
