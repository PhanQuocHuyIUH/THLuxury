import { useEffect, useMemo, useState } from 'react';
import { useAuthStore } from '@/store/auth';
import { listBranches } from '@/lib/branches';
import { listManagedOrders, statusBadge } from '@/lib/orders';
import { fmtVnd, shortId } from '@/lib/format';
import type { Branch, OrderSummary } from '@/types';

const REVENUE_STATUSES = new Set(['PAID', 'PREPARING', 'READY_FOR_PICKUP', 'SHIPPING', 'DELIVERED', 'COMPLETED']);

export default function StatsPage() {
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role === 'ADMIN';

  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [branchId, setBranchId] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listBranches().then(setBranches).catch(() => setBranches([]));
  }, []);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listManagedOrders({
        size: 200,
        page: 0,
        branchId: isAdmin && branchId ? branchId : undefined
      });
      setOrders(data.content);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Không tải được dữ liệu thống kê');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branchId]);

  const branchName = useMemo(() => {
    const map = new Map(branches.map((b) => [b.id, b.name]));
    return (id: string) => map.get(id) || shortId(id);
  }, [branches]);

  const agg = useMemo(() => {
    const total = orders.length;
    const revenue = orders
      .filter((o) => REVENUE_STATUSES.has(o.currentStatus))
      .reduce((s, o) => s + Number(o.total), 0);
    const byStatus = new Map<string, number>();
    const byBranch = new Map<string, { count: number; revenue: number }>();
    for (const o of orders) {
      byStatus.set(o.currentStatus, (byStatus.get(o.currentStatus) || 0) + 1);
      const cur = byBranch.get(o.branchId) || { count: 0, revenue: 0 };
      cur.count += 1;
      if (REVENUE_STATUSES.has(o.currentStatus)) cur.revenue += Number(o.total);
      byBranch.set(o.branchId, cur);
    }
    return { total, revenue, byStatus, byBranch };
  }, [orders]);

  return (
    <>
      <div className="admin-topbar">
        <h1>Thống kê</h1>
        <button className="btn btn-outline-primary btn-sm" onClick={load} disabled={loading}>
          {loading ? 'Đang tải...' : 'Tải lại'}
        </button>
      </div>

      {error && <div className="alert alert-danger py-2">{error}</div>}

      {isAdmin && (
        <div className="admin-card mb-3">
          <div className="row g-2 align-items-end">
            <div className="col-md-4">
              <label className="form-label mb-1">Chi nhánh</label>
              <select
                className="form-select form-select-sm"
                value={branchId}
                onChange={(e) => setBranchId(e.target.value)}
              >
                <option value="">Tất cả</option>
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} ({b.code})
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-8">
              <small className="text-muted">
                Tổng hợp trên 200 đơn gần nhất (do BE chưa có endpoint stats riêng).
              </small>
            </div>
          </div>
        </div>
      )}

      <div className="row g-3 mb-3">
        <div className="col-md-4">
          <div className="admin-card">
            <div className="text-muted">Tổng đơn (200 gần nhất)</div>
            <div className="display-6 fw-bold">{agg.total}</div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="admin-card">
            <div className="text-muted">Doanh thu (PAID+)</div>
            <div className="display-6 fw-bold">{fmtVnd(agg.revenue)}</div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="admin-card">
            <div className="text-muted">Đơn thành công</div>
            <div className="display-6 fw-bold">
              {orders.filter((o) => REVENUE_STATUSES.has(o.currentStatus)).length}
            </div>
          </div>
        </div>
      </div>

      <div className="row g-3">
        <div className="col-md-6">
          <div className="admin-card">
            <h5 className="mb-3">Theo trạng thái</h5>
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Trạng thái</th>
                  <th className="text-end">Số đơn</th>
                </tr>
              </thead>
              <tbody>
                {[...agg.byStatus.entries()].length === 0 && (
                  <tr>
                    <td colSpan={2} className="text-center text-muted py-3">
                      —
                    </td>
                  </tr>
                )}
                {[...agg.byStatus.entries()]
                  .sort((a, b) => b[1] - a[1])
                  .map(([s, n]) => {
                    const sb = statusBadge(s);
                    return (
                      <tr key={s}>
                        <td>
                          <span className={`badge bg-${sb.bg}`}>{sb.label}</span>
                        </td>
                        <td className="text-end">{n}</td>
                      </tr>
                    );
                  })}
              </tbody>
            </table>
          </div>
        </div>

        <div className="col-md-6">
          <div className="admin-card">
            <h5 className="mb-3">Theo chi nhánh</h5>
            <table className="table table-sm align-middle mb-0">
              <thead>
                <tr>
                  <th>Chi nhánh</th>
                  <th className="text-end">Đơn</th>
                  <th className="text-end">Doanh thu</th>
                </tr>
              </thead>
              <tbody>
                {[...agg.byBranch.entries()].length === 0 && (
                  <tr>
                    <td colSpan={3} className="text-center text-muted py-3">
                      —
                    </td>
                  </tr>
                )}
                {[...agg.byBranch.entries()]
                  .sort((a, b) => b[1].revenue - a[1].revenue)
                  .map(([id, v]) => (
                    <tr key={id}>
                      <td>{branchName(id)}</td>
                      <td className="text-end">{v.count}</td>
                      <td className="text-end">{fmtVnd(v.revenue)}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </>
  );
}
