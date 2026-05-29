import { FormEvent, useEffect, useMemo, useState } from 'react';
import { listBranches } from '@/lib/branches';
import {
  createBranchManager,
  listUsers,
  updateUser
} from '@/lib/users';
import { fmtDateTime, shortId } from '@/lib/format';
import type {
  Branch,
  CreateBranchManagerRequest,
  Role,
  UserView
} from '@/types';

const EMPTY_MANAGER: CreateBranchManagerRequest = {
  email: '',
  password: '',
  fullName: '',
  phone: '',
  branchId: ''
};

export default function UsersPage() {
  const [items, setItems] = useState<UserView[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [role, setRole] = useState<'' | Role>('');
  const [branchId, setBranchId] = useState('');
  const [branches, setBranches] = useState<Branch[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<CreateBranchManagerRequest>(EMPTY_MANAGER);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    listBranches().then(setBranches).catch(() => setBranches([]));
  }, []);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listUsers({
        page,
        size,
        role: role || undefined,
        branchId: branchId || undefined
      });
      setItems(data.content);
      setTotal(data.total);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Không tải được danh sách người dùng');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, role, branchId]);

  const branchName = useMemo(() => {
    const map = new Map(branches.map((b) => [b.id, b.name]));
    return (id?: string | null) => (id ? map.get(id) || shortId(id) : '—');
  }, [branches]);

  const handleCreateManager = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.branchId) {
      setError('Cần chọn chi nhánh');
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await createBranchManager(form);
      setForm(EMPTY_MANAGER);
      setShowForm(false);
      setPage(0);
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Tạo manager thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const handleToggleEnabled = async (u: UserView) => {
    if (!confirm(`${u.enabled ? 'Khoá' : 'Mở khoá'} tài khoản ${u.email}?`)) return;
    try {
      await updateUser(u.id, { enabled: !u.enabled });
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Cập nhật thất bại');
    }
  };

  const totalPages = Math.max(1, Math.ceil(total / size));

  return (
    <>
      <div className="admin-topbar">
        <h1>Người dùng</h1>
        <div className="d-flex gap-2">
          <button
            className="btn btn-primary btn-sm"
            onClick={() => setShowForm((v) => !v)}
          >
            {showForm ? 'Đóng form' : '+ Tạo Branch Manager'}
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

      <div className="admin-card mb-3">
        <div className="row g-2 align-items-end">
          <div className="col-md-3">
            <label className="form-label mb-1">Vai trò</label>
            <select
              className="form-select form-select-sm"
              value={role}
              onChange={(e) => {
                setRole(e.target.value as Role | '');
                setPage(0);
              }}
            >
              <option value="">Tất cả</option>
              <option value="ADMIN">ADMIN</option>
              <option value="BRANCH_MANAGER">BRANCH_MANAGER</option>
              <option value="CUSTOMER">CUSTOMER</option>
            </select>
          </div>
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
          <div className="col-md-3">
            <small className="text-muted">
              {total} người dùng · trang {page + 1}/{totalPages}
            </small>
          </div>
        </div>
      </div>

      {showForm && (
        <div className="admin-card mb-3">
          <h5 className="mb-3">Tạo Branch Manager</h5>
          <form className="row g-3" onSubmit={handleCreateManager}>
            <div className="col-md-4">
              <label className="form-label">Email</label>
              <input
                type="email"
                className="form-control"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                required
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Họ tên</label>
              <input
                className="form-control"
                value={form.fullName}
                onChange={(e) => setForm({ ...form, fullName: e.target.value })}
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
            <div className="col-md-4">
              <label className="form-label">Mật khẩu</label>
              <input
                type="password"
                className="form-control"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                required
                minLength={8}
              />
            </div>
            <div className="col-md-4">
              <label className="form-label">Chi nhánh phụ trách</label>
              <select
                className="form-select"
                value={form.branchId}
                onChange={(e) => setForm({ ...form, branchId: e.target.value })}
                required
              >
                <option value="">— Chọn —</option>
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.name} ({b.code})
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-12">
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Đang tạo...' : 'Tạo manager'}
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
                <th>Email</th>
                <th>Họ tên</th>
                <th>Vai trò</th>
                <th>Chi nhánh</th>
                <th>Ngày tạo</th>
                <th>Trạng thái</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 && (
                <tr>
                  <td colSpan={7} className="text-center text-muted py-4">
                    {loading ? 'Đang tải...' : 'Không có người dùng'}
                  </td>
                </tr>
              )}
              {items.map((u) => (
                <tr key={u.id}>
                  <td>{u.email}</td>
                  <td>{u.fullName}</td>
                  <td>
                    <span
                      className={`badge bg-${
                        u.role === 'ADMIN'
                          ? 'danger'
                          : u.role === 'BRANCH_MANAGER'
                          ? 'primary'
                          : 'secondary'
                      }`}
                    >
                      {u.role}
                    </span>
                  </td>
                  <td>{branchName(u.branchId)}</td>
                  <td>{fmtDateTime(u.createdAt)}</td>
                  <td>
                    {u.enabled ? (
                      <span className="badge bg-success">Hoạt động</span>
                    ) : (
                      <span className="badge bg-secondary">Bị khoá</span>
                    )}
                  </td>
                  <td className="text-end">
                    <button
                      className={`btn btn-sm btn-outline-${u.enabled ? 'danger' : 'success'}`}
                      onClick={() => handleToggleEnabled(u)}
                    >
                      {u.enabled ? 'Khoá' : 'Mở khoá'}
                    </button>
                  </td>
                </tr>
              ))}
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
