import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import AdminShell from '@/components/AdminShell';
import ProtectedRoute from '@/components/ProtectedRoute';
import LoginPage from '@/pages/Login';
import DashboardPage from '@/pages/Dashboard';
import VouchersPage from '@/pages/Vouchers';
import OrdersPage from '@/pages/Orders';
import InventoryPage from '@/pages/Inventory';
import ProductsPage from '@/pages/Products';
import UsersPage from '@/pages/Users';
import BranchesPage from '@/pages/Branches';
import StatsPage from '@/pages/Stats';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route
          element={
            <ProtectedRoute>
              <AdminShell />
            </ProtectedRoute>
          }
        >
          <Route index element={<DashboardPage />} />
          <Route path="orders" element={<OrdersPage />} />
          <Route
            path="products"
            element={
              <ProtectedRoute allow={['ADMIN']}>
                <ProductsPage />
              </ProtectedRoute>
            }
          />
          <Route path="inventory" element={<InventoryPage />} />
          <Route path="vouchers" element={<VouchersPage />} />
          <Route
            path="users"
            element={
              <ProtectedRoute allow={['ADMIN']}>
                <UsersPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="branches"
            element={
              <ProtectedRoute allow={['ADMIN']}>
                <BranchesPage />
              </ProtectedRoute>
            }
          />
          <Route path="stats" element={<StatsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}
