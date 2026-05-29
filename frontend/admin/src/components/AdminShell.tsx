import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/store/auth';
import { NAV_ITEMS } from '@/lib/nav';

export default function AdminShell() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const clear = useAuthStore((s) => s.clear);

  const items = NAV_ITEMS.filter((n) => user && n.roles.includes(user.role));

  const handleLogout = () => {
    clear();
    navigate('/login', { replace: true });
  };

  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="brand">THLUXURY · ADMIN</div>
        <nav>
          {items.map((it) => (
            <NavLink
              key={it.to}
              to={it.to}
              end={it.to === '/'}
              className={({ isActive }) => (isActive ? 'active' : '')}
            >
              {it.label}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-footer">
          <div>{user?.fullName}</div>
          <div style={{ opacity: 0.7, fontSize: 12 }}>{user?.role}</div>
          <button
            onClick={handleLogout}
            className="btn btn-outline-light btn-sm mt-2 w-100"
          >
            Đăng xuất
          </button>
        </div>
      </aside>
      <main className="admin-content">
        <Outlet />
      </main>
    </div>
  );
}
