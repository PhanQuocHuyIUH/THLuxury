import { FormEvent, useEffect, useState } from 'react';
import { useAuthStore } from '@/store/auth';
import { createVoucher, disableVoucher, listVouchers } from '@/lib/vouchers';
import type { CreateVoucherRequest, Voucher } from '@/types';

const EMPTY_FORM: CreateVoucherRequest = {
  code: '',
  type: 'PERCENT',
  value: 10,
  minOrderValue: 0,
  maxDiscount: null,
  expiresAt: null,
  usageLimit: null,
  enabled: true
};

export default function VouchersPage() {
  const role = useAuthStore((s) => s.user?.role);
  const isAdmin = role === 'ADMIN';

  const [items, setItems] = useState<Voucher[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState<CreateVoucherRequest>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await listVouchers());
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Không tải được danh sách voucher');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await createVoucher({
        ...form,
        code: form.code.trim().toUpperCase(),
        expiresAt: form.expiresAt ? new Date(form.expiresAt).toISOString() : null
      });
      setForm(EMPTY_FORM);
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Tạo voucher thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDisable = async (v: Voucher) => {
    if (!confirm(`Vô hiệu voucher ${v.code}?`)) return;
    try {
      await disableVoucher(v.id);
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Vô hiệu voucher thất bại');
    }
  };

  return (
    <>
      <div className="admin-topbar">
        <h1>Vouchers</h1>
        <button className="btn btn-outline-primary btn-sm" onClick={load} disabled={loading}>
          {loading ? 'Đang tải...' : 'Tải lại'}
        </button>
      </div>

      {error && <div className="alert alert-danger py-2">{error}</div>}

      {isAdmin && (
        <div className="admin-card mb-3">
          <h5 className="mb-3">Tạo voucher</h5>
          <form className="row g-3" onSubmit={handleCreate}>
            <div className="col-md-3">
              <label className="form-label">Mã</label>
              <input
                className="form-control"
                value={form.code}
                onChange={(e) => setForm({ ...form, code: e.target.value })}
                required
                maxLength={50}
              />
            </div>
            <div className="col-md-2">
              <label className="form-label">Loại</label>
              <select
                className="form-select"
                value={form.type}
                onChange={(e) =>
                  setForm({ ...form, type: e.target.value as 'PERCENT' | 'FIXED' })
                }
              >
                <option value="PERCENT">% giảm</option>
                <option value="FIXED">Giảm VND</option>
              </select>
            </div>
            <div className="col-md-2">
              <label className="form-label">Giá trị</label>
              <input
                type="number"
                className="form-control"
                value={form.value}
                onChange={(e) => setForm({ ...form, value: Number(e.target.value) })}
                min={0}
                step={form.type === 'PERCENT' ? 1 : 1000}
                required
              />
            </div>
            <div className="col-md-2">
              <label className="form-label">Đơn tối thiểu</label>
              <input
                type="number"
                className="form-control"
                value={form.minOrderValue ?? 0}
                onChange={(e) =>
                  setForm({ ...form, minOrderValue: Number(e.target.value) })
                }
                min={0}
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Hết hạn</label>
              <input
                type="datetime-local"
                className="form-control"
                value={form.expiresAt ?? ''}
                onChange={(e) =>
                  setForm({ ...form, expiresAt: e.target.value || null })
                }
              />
            </div>
            <div className="col-md-2">
              <label className="form-label">Giảm tối đa</label>
              <input
                type="number"
                className="form-control"
                value={form.maxDiscount ?? ''}
                onChange={(e) =>
                  setForm({
                    ...form,
                    maxDiscount: e.target.value ? Number(e.target.value) : null
                  })
                }
                min={0}
              />
            </div>
            <div className="col-md-2">
              <label className="form-label">Lượt dùng</label>
              <input
                type="number"
                className="form-control"
                value={form.usageLimit ?? ''}
                onChange={(e) =>
                  setForm({
                    ...form,
                    usageLimit: e.target.value ? Number(e.target.value) : null
                  })
                }
                min={1}
              />
            </div>
            <div className="col-md-2 d-flex align-items-end">
              <button type="submit" className="btn btn-primary w-100" disabled={submitting}>
                {submitting ? 'Đang tạo...' : 'Tạo'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="admin-card">
        <h5 className="mb-3">Danh sách</h5>
        <div className="table-responsive">
          <table className="table table-sm align-middle">
            <thead>
              <tr>
                <th>Mã</th>
                <th>Loại</th>
                <th className="text-end">Giá trị</th>
                <th className="text-end">Đơn tối thiểu</th>
                <th className="text-end">Giảm tối đa</th>
                <th>Hết hạn</th>
                <th className="text-end">Đã dùng / Hạn mức</th>
                <th>Trạng thái</th>
                {isAdmin && <th></th>}
              </tr>
            </thead>
            <tbody>
              {items.length === 0 && (
                <tr>
                  <td colSpan={isAdmin ? 9 : 8} className="text-center text-muted py-4">
                    {loading ? 'Đang tải...' : 'Chưa có voucher'}
                  </td>
                </tr>
              )}
              {items.map((v) => (
                <tr key={v.id}>
                  <td>
                    <code>{v.code}</code>
                  </td>
                  <td>{v.type === 'PERCENT' ? '% giảm' : 'Giảm VND'}</td>
                  <td className="text-end">
                    {v.type === 'PERCENT'
                      ? `${v.value}%`
                      : v.value.toLocaleString('vi-VN')}
                  </td>
                  <td className="text-end">{v.minOrderValue.toLocaleString('vi-VN')}</td>
                  <td className="text-end">
                    {v.maxDiscount ? v.maxDiscount.toLocaleString('vi-VN') : '—'}
                  </td>
                  <td>
                    {v.expiresAt ? new Date(v.expiresAt).toLocaleString('vi-VN') : '—'}
                  </td>
                  <td className="text-end">
                    {v.usedCount} / {v.usageLimit ?? '∞'}
                  </td>
                  <td>
                    {v.enabled ? (
                      <span className="badge bg-success">Đang bật</span>
                    ) : (
                      <span className="badge bg-secondary">Vô hiệu</span>
                    )}
                  </td>
                  {isAdmin && (
                    <td className="text-end">
                      {v.enabled && (
                        <button
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => handleDisable(v)}
                        >
                          Vô hiệu
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}
