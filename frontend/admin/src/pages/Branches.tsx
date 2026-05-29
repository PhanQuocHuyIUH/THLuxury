import { FormEvent, useEffect, useState } from 'react';
import { createBranch, listBranches, updateBranch } from '@/lib/branches';
import { shortId } from '@/lib/format';
import type { Branch, CreateBranchRequest } from '@/types';

const EMPTY_FORM: CreateBranchRequest = {
  code: '',
  name: '',
  address: '',
  city: '',
  district: '',
  ward: '',
  phone: ''
};

export default function BranchesPage() {
  const [items, setItems] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<CreateBranchRequest>(EMPTY_FORM);
  const [submitting, setSubmitting] = useState(false);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      setItems(await listBranches());
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Không tải được danh sách chi nhánh');
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
      await createBranch({
        ...form,
        code: form.code.trim().toUpperCase(),
        name: form.name.trim()
      });
      setForm(EMPTY_FORM);
      setShowForm(false);
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Tạo chi nhánh thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggle = async (b: Branch) => {
    if (!confirm(`${b.enabled ? 'Tắt' : 'Bật'} chi nhánh ${b.name}?`)) return;
    try {
      await updateBranch(b.id, { enabled: !b.enabled });
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Cập nhật chi nhánh thất bại');
    }
  };

  return (
    <>
      <div className="admin-topbar">
        <h1>Chi nhánh</h1>
        <div className="d-flex gap-2">
          <button
            className="btn btn-primary btn-sm"
            onClick={() => setShowForm((v) => !v)}
          >
            {showForm ? 'Đóng form' : '+ Thêm chi nhánh'}
          </button>
          <button
            className="btn btn-outline-primary btn-sm"
            onClick={load}
            disabled={loading}
          >
            {loading ? 'Đang tải...' : 'Tải lại'}
          </button>
        </div>
      </div>

      {error && <div className="alert alert-danger py-2">{error}</div>}

      {showForm && (
        <div className="admin-card mb-3">
          <h5 className="mb-3">Tạo chi nhánh</h5>
          <form className="row g-3" onSubmit={handleCreate}>
            <div className="col-md-3">
              <label className="form-label">Mã</label>
              <input
                className="form-control"
                value={form.code}
                onChange={(e) => setForm({ ...form, code: e.target.value })}
                required
                maxLength={16}
                placeholder="HCM01"
              />
            </div>
            <div className="col-md-5">
              <label className="form-label">Tên</label>
              <input
                className="form-control"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
                maxLength={200}
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">SĐT</label>
              <input
                className="form-control"
                value={form.phone || ''}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
              />
            </div>
            <div className="col-md-12">
              <label className="form-label">Địa chỉ</label>
              <input
                className="form-control"
                value={form.address || ''}
                onChange={(e) => setForm({ ...form, address: e.target.value })}
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Thành phố</label>
              <input
                className="form-control"
                value={form.city || ''}
                onChange={(e) => setForm({ ...form, city: e.target.value })}
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Quận / Huyện</label>
              <input
                className="form-control"
                value={form.district || ''}
                onChange={(e) => setForm({ ...form, district: e.target.value })}
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Phường / Xã</label>
              <input
                className="form-control"
                value={form.ward || ''}
                onChange={(e) => setForm({ ...form, ward: e.target.value })}
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Lat</label>
              <input
                type="number"
                className="form-control"
                step="0.000001"
                value={form.lat ?? ''}
                onChange={(e) =>
                  setForm({ ...form, lat: e.target.value ? Number(e.target.value) : null })
                }
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Lng</label>
              <input
                type="number"
                className="form-control"
                step="0.000001"
                value={form.lng ?? ''}
                onChange={(e) =>
                  setForm({ ...form, lng: e.target.value ? Number(e.target.value) : null })
                }
              />
            </div>
            <div className="col-md-12">
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Đang tạo...' : 'Tạo chi nhánh'}
              </button>
            </div>
          </form>
        </div>
      )}

      <div className="admin-card">
        <div className="table-responsive">
          <table className="table table-sm align-middle">
            <thead>
              <tr>
                <th>Mã</th>
                <th>Tên</th>
                <th>Địa chỉ</th>
                <th>SĐT</th>
                <th>Trạng thái</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 && (
                <tr>
                  <td colSpan={6} className="text-center text-muted py-4">
                    {loading ? 'Đang tải...' : 'Chưa có chi nhánh'}
                  </td>
                </tr>
              )}
              {items.map((b) => (
                <tr key={b.id}>
                  <td>
                    <code>{b.code}</code>
                    <div>
                      <small className="text-muted">{shortId(b.id, 8)}</small>
                    </div>
                  </td>
                  <td>{b.name}</td>
                  <td>
                    {[b.address, b.ward, b.district, b.city].filter(Boolean).join(', ') || '—'}
                  </td>
                  <td>{b.phone || '—'}</td>
                  <td>
                    {b.enabled ? (
                      <span className="badge bg-success">Đang bật</span>
                    ) : (
                      <span className="badge bg-secondary">Tắt</span>
                    )}
                  </td>
                  <td className="text-end">
                    <button
                      className={`btn btn-sm btn-outline-${b.enabled ? 'danger' : 'success'}`}
                      onClick={() => handleToggle(b)}
                    >
                      {b.enabled ? 'Tắt' : 'Bật'}
                    </button>
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
