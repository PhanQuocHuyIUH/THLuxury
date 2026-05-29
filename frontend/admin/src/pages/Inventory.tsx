import { FormEvent, useEffect, useMemo, useState } from 'react';
import { useAuthStore } from '@/store/auth';
import { listBranches } from '@/lib/branches';
import { listProducts } from '@/lib/products';
import { listInventory, listLowStock, stockIn } from '@/lib/inventory';
import { shortId } from '@/lib/format';
import type { Branch, InventoryRow, ProductSummary } from '@/types';

export default function InventoryPage() {
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.role === 'ADMIN';

  const [branches, setBranches] = useState<Branch[]>([]);
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [branchId, setBranchId] = useState<string>('');
  const [rows, setRows] = useState<InventoryRow[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [onlyLow, setOnlyLow] = useState(false);
  const [threshold, setThreshold] = useState(5);

  const [stockProductId, setStockProductId] = useState('');
  const [stockQty, setStockQty] = useState(1);
  const [stockRef, setStockRef] = useState('');
  const [stockInBranchId, setStockInBranchId] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    listBranches().then(setBranches).catch(() => setBranches([]));
    listProducts({ size: 1000 }).then(res => setProducts(res.content)).catch(() => setProducts([]));
  }, []);

  const effectiveBranch = isAdmin ? branchId : user?.branchId || '';
  const effectiveStockInBranch = isAdmin ? stockInBranchId : user?.branchId || '';

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = onlyLow
        ? await listLowStock(isAdmin ? branchId || undefined : undefined, threshold)
        : await listInventory(isAdmin ? branchId || undefined : undefined);
      setRows(data);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Không tải được tồn kho');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [branchId, onlyLow, threshold]);

  const branchName = useMemo(() => {
    const map = new Map(branches.map((b) => [b.id, b.name]));
    return (id: string) => map.get(id) || shortId(id);
  }, [branches]);

  const productName = useMemo(() => {
    const map = new Map(products.map((p) => [p.id, p.tenSp]));
    return (id: string) => map.get(id) || shortId(id);
  }, [products]);

  const handleStockIn = async (e: FormEvent) => {
    e.preventDefault();
    if (!stockProductId.trim() || !effectiveStockInBranch) {
      setError('Cần chọn sản phẩm và chọn chi nhánh để nhập kho');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await stockIn({
        productId: stockProductId.trim(),
        branchId: effectiveStockInBranch,
        quantity: stockQty,
        reference: stockRef.trim() || undefined
      });
      setStockProductId('');
      setStockQty(1);
      setStockRef('');
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Nhập kho thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <div className="admin-topbar">
        <h1>Tồn kho</h1>
        <button className="btn btn-outline-primary btn-sm" onClick={load} disabled={loading}>
          {loading ? 'Đang tải...' : 'Tải lại'}
        </button>
      </div>

      {error && <div className="alert alert-danger py-2">{error}</div>}

      <div className="admin-card mb-3">
        <div className="row g-2 align-items-end">
          {isAdmin && (
            <div className="col-md-3">
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
          )}
          <div className="col-md-2">
            <div className="form-check mt-3">
              <input
                className="form-check-input"
                type="checkbox"
                id="onlyLow"
                checked={onlyLow}
                onChange={(e) => setOnlyLow(e.target.checked)}
              />
              <label className="form-check-label" htmlFor="onlyLow">
                Chỉ tồn thấp
              </label>
            </div>
          </div>
          {onlyLow && (
            <div className="col-md-2">
              <label className="form-label mb-1">Ngưỡng</label>
              <input
                type="number"
                className="form-control form-control-sm"
                value={threshold}
                min={0}
                onChange={(e) => setThreshold(Number(e.target.value))}
              />
            </div>
          )}
        </div>
      </div>

      <div className="admin-card mb-3">
        <h5 className="mb-3">Nhập kho</h5>
        <form className="row g-2 align-items-end" onSubmit={handleStockIn}>
          {isAdmin ? (
            <div className="col-md-3">
              <label className="form-label mb-1">Chi nhánh nhập</label>
              <select
                className="form-select form-select-sm"
                value={stockInBranchId}
                onChange={(e) => setStockInBranchId(e.target.value)}
                required
              >
                <option value="">— Chọn chi nhánh —</option>
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name}
                  </option>
                ))}
              </select>
            </div>
          ) : (
            <div className="col-md-3">
              <label className="form-label mb-1">Chi nhánh của bạn</label>
              <input
                className="form-control form-control-sm bg-light"
                value={branchName(user?.branchId || '')}
                disabled
              />
            </div>
          )}
          <div className="col-md-3">
            <label className="form-label mb-1">Sản phẩm</label>
            <select
              className="form-select form-select-sm"
              value={stockProductId}
              onChange={(e) => setStockProductId(e.target.value)}
              required
            >
              <option value="">— Chọn sản phẩm —</option>
              {products.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.tenSp}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-2">
            <label className="form-label mb-1">Số lượng</label>
            <input
              type="number"
              className="form-control form-control-sm"
              value={stockQty}
              min={1}
              onChange={(e) => setStockQty(Number(e.target.value))}
              required
            />
          </div>
          <div className="col-md-2">
            <label className="form-label mb-1">Mã phiếu nhập (Tùy chọn)</label>
            <input
              className="form-control form-control-sm"
              value={stockRef}
              onChange={(e) => setStockRef(e.target.value)}
              placeholder="PO-001"
            />
          </div>
          <div className="col-md-2">
            <button className="btn btn-primary btn-sm w-100" disabled={submitting}>
              {submitting ? 'Đang nhập...' : 'Nhập kho'}
            </button>
          </div>
        </form>
      </div>

      <div className="admin-card">
        <div className="table-responsive">
          <table className="table table-sm align-middle">
            <thead>
              <tr>
                <th>Sản phẩm</th>
                <th>Chi nhánh</th>
                <th className="text-end">Tồn</th>
                <th className="text-end">Đang giữ</th>
                <th className="text-end">Khả dụng</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 && (
                <tr>
                  <td colSpan={5} className="text-center text-muted py-4">
                    {loading ? 'Đang tải...' : 'Không có dữ liệu'}
                  </td>
                </tr>
              )}
              {rows.map((r) => (
                <tr key={r.id}>
                  <td>{productName(r.productId)}</td>
                  <td>{branchName(r.branchId)}</td>
                  <td className="text-end">{r.quantity}</td>
                  <td className="text-end">{r.reservedQuantity}</td>
                  <td className="text-end">
                    <strong>{r.quantity - r.reservedQuantity}</strong>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}