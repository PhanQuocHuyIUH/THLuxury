import React, { useEffect, useMemo, useState } from 'react';
import { useAuthStore } from '@/store/auth';
import { listManagedOrders, updateOrderStatus, statusBadge, getOrderDetail } from '@/lib/orders';
import { listBranches } from '@/lib/branches';
import { fmtDateTime, fmtVnd, shortId } from '@/lib/format';
import { Link } from 'react-router-dom';
import type { Branch, OrderSummary } from '@/types';

const ACTIONABLE_STATUSES = ['PAID', 'CONFIRMED', 'PREPARING'];

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role === 'ADMIN';

  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actingId, setActingId] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [details, setDetails] = useState<Record<string, any>>({});

  useEffect(() => {
    listBranches().then(setBranches).catch(() => setBranches([]));
  }, []);

  const loadOrders = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listManagedOrders({
        page: 0,
        size: 50,
        statuses: ACTIONABLE_STATUSES,
        branchId: isAdmin ? undefined : (user?.branchId || undefined)
      });
      setOrders(data.content);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Không tải được danh sách đơn hàng mới');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadOrders();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const doTransition = async (maDh: string, target: string) => {
    setActingId(maDh);
    setError(null);
    try {
      await updateOrderStatus(maDh, target);
      await loadOrders();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Không cập nhật được trạng thái đơn');
    } finally {
      setActingId(null);
    }
  };

  const toggleDetail = async (maDh: string) => {
    if (expandedId === maDh) {
      setExpandedId(null);
      return;
    }
    setExpandedId(maDh);
    if (!details[maDh]) {
      try {
        const d = await getOrderDetail(maDh);
        setDetails(prev => ({ ...prev, [maDh]: d }));
      } catch (e) {
        console.error('Failed to load detail', e);
      }
    }
  };

  const branchName = useMemo(() => {
    const map = new Map(branches.map((b) => [b.id, b.name]));
    return (id: string) => map.get(id) || shortId(id);
  }, [branches]);

  const stats = useMemo(() => {
    if (!isAdmin) {
      return { total: orders.length };
    }
    const counts = new Map<string, number>();
    for (const o of orders) {
      counts.set(o.branchId, (counts.get(o.branchId) || 0) + 1);
    }
    return {
      total: orders.length,
      branches: branches.map(b => ({
        ...b,
        count: counts.get(b.id) || 0
      })).filter(b => b.count > 0).sort((a, b) => b.count - a.count)
    };
  }, [orders, isAdmin, branches]);

  return (
    <>
      <div className="admin-topbar">
        <h1>Dashboard</h1>
        <button className="btn btn-outline-primary btn-sm" onClick={loadOrders} disabled={loading}>
          {loading ? 'Đang tải...' : 'Tải lại'}
        </button>
      </div>

      <div className="row g-3 mb-4">
        <div className="col-md-6">
          <div className="admin-card h-100 d-flex flex-column justify-content-center">
            <h5 className="mb-1">
              Xin chào, <strong>{user?.fullName}</strong>.
            </h5>
            <p className="text-muted mb-0">
              Quyền: {user?.role}
              {user?.branchId ? ` · Chi nhánh: ${branchName(user.branchId)}` : ''}
            </p>
          </div>
        </div>
        <div className="col-md-6">
          <div className="admin-card h-100">
            <h5 className="mb-3 text-primary">Tóm tắt công việc</h5>
            {isAdmin ? (
              <div>
                <div className="mb-2"><strong>Tổng:</strong> {stats.total} đơn cần xử lý</div>
                <div className="d-flex flex-wrap gap-2">
                  {stats.branches?.map(b => (
                    <Link key={b.id} to={`/orders?branchId=${b.id}`} className="badge bg-info text-decoration-none p-2 fs-6">
                      {b.name}: {b.count}
                    </Link>
                  ))}
                  {stats.branches?.length === 0 && <span className="text-muted">Chưa có đơn hàng nào cần xử lý.</span>}
                </div>
              </div>
            ) : (
              <div>
                <h2 className="mb-0 text-success">{stats.total}</h2>
                <div className="text-muted">Đơn hàng đang chờ xử lý tại chi nhánh của bạn.</div>
              </div>
            )}
          </div>
        </div>
      </div>

      {error && <div className="alert alert-danger py-2">{error}</div>}

      <div className="admin-card">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h5 className="mb-0">Đơn hàng cần hành động (Mới nhất)</h5>
        </div>
        <div className="table-responsive">
          <table className="table table-sm align-middle">
            <thead>
              <tr>
                <th>Mã ĐH</th>
                <th>Ngày tạo</th>
                <th>Chi nhánh</th>
                <th>Giao</th>
                <th className="text-end">Tổng</th>
                <th>Voucher</th>
                <th>Trạng thái</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {orders.length === 0 && (
                <tr>
                  <td colSpan={8} className="text-center text-muted py-4">
                    {loading ? 'Đang tải...' : 'Không có đơn cần xử lý'}
                  </td>
                </tr>
              )}
              {orders.map((o) => {
                const sb = statusBadge(o.currentStatus);
                const isExpanded = expandedId === o.maDh;
                const d = details[o.maDh];
                return (
                  <React.Fragment key={o.id}>
                    <tr>
                      <td>
                        <code>{o.maDh}</code>
                      </td>
                      <td>{fmtDateTime(o.createdAt)}</td>
                      <td>{branchName(o.branchId)}</td>
                      <td>{o.deliveryType ?? '—'}</td>
                      <td className="text-end">{fmtVnd(o.total)}</td>
                      <td>{o.voucherCode || '—'}</td>
                      <td>
                        <span className={`badge bg-${sb.bg}`}>{sb.label}</span>
                      </td>
                      <td>
                        <div className="d-flex flex-wrap gap-1">
                          <button
                            className="btn btn-sm btn-outline-info"
                            onClick={() => toggleDetail(o.maDh)}
                          >
                            {isExpanded ? 'Đóng' : 'Chi tiết'}
                          </button>
                          {(o.nextStatuses ?? []).length === 0 && (
                            <span className="text-muted small d-none d-lg-inline-block align-self-center">—</span>
                          )}
                          {(o.nextStatuses ?? []).map((ns) => {
                            const nb = statusBadge(ns);
                            const isCancel = ns === 'CANCELLED';
                            return (
                              <button
                                key={ns}
                                className={`btn btn-sm ${isCancel ? 'btn-outline-danger' : 'btn-outline-primary'}`}
                                disabled={actingId === o.maDh}
                                onClick={() => doTransition(o.maDh, ns)}
                              >
                                {actingId === o.maDh ? '...' : nb.label}
                              </button>
                            );
                          })}
                        </div>
                      </td>
                    </tr>
                    {isExpanded && (
                      <tr>
                        <td colSpan={8} className="bg-light">
                          {d ? (
                            <div className="p-2 border rounded bg-white small">
                              <div className="row">
                                <div className="col-md-4">
                                  <strong>Khách hàng:</strong>
                                  <pre className="mb-0 mt-1" style={{ whiteSpace: 'pre-wrap' }}>
                                    {d.customerSnapshot ? JSON.stringify(JSON.parse(d.customerSnapshot), null, 2) : '—'}
                                  </pre>
                                </div>
                                <div className="col-md-4">
                                  <strong>Địa chỉ giao hàng:</strong>
                                  <pre className="mb-0 mt-1" style={{ whiteSpace: 'pre-wrap' }}>
                                    {d.addressSnapshot ? JSON.stringify(JSON.parse(d.addressSnapshot), null, 2) : '—'}
                                  </pre>
                                </div>
                                <div className="col-md-4">
                                  <strong>Sản phẩm:</strong>
                                  <div className="mt-1">
                                    {d.itemsSnapshot ? JSON.parse(d.itemsSnapshot).map((it: any, i: number) => (
                                      <div key={i} className="mb-1 pb-1 border-bottom">
                                        <div>{it.tenSp} (x{it.soLuong})</div>
                                        <div className="text-muted">{fmtVnd(it.giaMua)}</div>
                                      </div>
                                    )) : '—'}
                                  </div>
                                </div>
                              </div>
                            </div>
                          ) : (
                            <div className="text-center text-muted p-2">Đang tải...</div>
                          )}
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}