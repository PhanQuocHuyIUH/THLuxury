import React, { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuthStore } from '@/store/auth';
import { listManagedOrders, updateOrderStatus, ORDER_STATUSES, statusBadge, getOrderDetail } from '@/lib/orders';
import { listBranches } from '@/lib/branches';
import { fmtDateTime, fmtVnd, shortId } from '@/lib/format';
import type { Branch, OrderSummary } from '@/types';

export default function OrdersPage() {
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role === 'ADMIN';

  const [searchParams, setSearchParams] = useSearchParams();

  const [items, setItems] = useState<OrderSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [status, setStatus] = useState<string>(searchParams.get('status') || '');
  const [branchId, setBranchId] = useState<string>(searchParams.get('branchId') || '');
  const [branches, setBranches] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [actingId, setActingId] = useState<string | null>(null);
  
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [details, setDetails] = useState<Record<string, any>>({});

  useEffect(() => {
    listBranches().then(setBranches).catch(() => setBranches([]));
  }, []);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listManagedOrders({
        page,
        size,
        statuses: status ? [status] : undefined,
        branchId: isAdmin && branchId ? branchId : undefined
      });
      setItems(data.content);
      setTotal(data.total);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Không tải được danh sách đơn');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, status, branchId]);

  const doTransition = async (maDh: string, target: string) => {
    setActingId(maDh);
    setError(null);
    try {
      await updateOrderStatus(maDh, target);
      await load();
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

  const totalPages = Math.max(1, Math.ceil(total / size));

  return (
    <>
      <div className="admin-topbar">
        <h1>Đơn hàng</h1>
        <button className="btn btn-outline-primary btn-sm" onClick={load} disabled={loading}>
          {loading ? 'Đang tải...' : 'Tải lại'}
        </button>
      </div>

      {error && <div className="alert alert-danger py-2">{error}</div>}

      <div className="admin-card mb-3">
        <div className="row g-2 align-items-end">
          <div className="col-md-3">
            <label className="form-label mb-1">Trạng thái</label>
            <select
              className="form-select form-select-sm"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Tất cả</option>
              {ORDER_STATUSES.map((s) => (
                <option key={s} value={s}>
                  {statusBadge(s).label}
                </option>
              ))}
            </select>
          </div>
          {isAdmin && (
            <div className="col-md-3">
              <label className="form-label mb-1">Chi nhánh</label>
              <select
                className="form-select form-select-sm"
                value={branchId}
                onChange={(e) => {
                  setBranchId(e.target.value);
                  setPage(0);
                }}
              >
                <option value="">Tất cả</option>
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} ({b.code})
                  </option>
                ))}
              </select>
            </div>
          )}
          {!isAdmin && user?.branchId && (
            <div className="col-md-3">
              <label className="form-label mb-1">Chi nhánh của bạn</label>
              <div className="form-control form-control-sm bg-light">
                {branchName(user.branchId)}
              </div>
            </div>
          )}
          <div className="col-md-3">
            <small className="text-muted">
              {total} đơn · trang {page + 1}/{totalPages}
            </small>
          </div>
        </div>
      </div>

      <div className="admin-card">
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
              {items.length === 0 && (
                <tr>
                  <td colSpan={8} className="text-center text-muted py-4">
                    {loading ? 'Đang tải...' : 'Không có đơn'}
                  </td>
                </tr>
              )}
              {items.map((o) => {
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

        <div className="d-flex justify-content-between mt-3">
          <button
            className="btn btn-outline-secondary btn-sm"
            disabled={page === 0 || loading}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
          >
            ← Trước
          </button>
          <button
            className="btn btn-outline-secondary btn-sm"
            disabled={page + 1 >= totalPages || loading}
            onClick={() => setPage((p) => p + 1)}
          >
            Sau →
          </button>
        </div>
      </div>
    </>
  );
}
