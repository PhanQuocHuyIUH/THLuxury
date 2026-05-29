import { FormEvent, useEffect, useState } from 'react';
import {
  archiveProduct,
  createProduct,
  listCategories,
  listProducts
} from '@/lib/products';
import { fmtVnd, shortId } from '@/lib/format';
import type { CreateProductRequest, ProductSummary } from '@/types';

const EMPTY_FORM: CreateProductRequest = {
  tenSp: '',
  loaiSp: '',
  giaBanDau: 0,
  giaGiamGia: 0,
  description: '',
  images: []
};

export default function ProductsPage() {
  const [items, setItems] = useState<ProductSummary[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [keyword, setKeyword] = useState('');
  const [loaiSp, setLoaiSp] = useState('');
  const [categories, setCategories] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState<CreateProductRequest>(EMPTY_FORM);
  const [imageUrl, setImageUrl] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    listCategories().then(setCategories).catch(() => setCategories([]));
  }, []);

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await listProducts({
        page,
        size,
        keyword: keyword || undefined,
        loaiSp: loaiSp || undefined
      });
      setItems(data.content);
      setTotal(data.total);
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Không tải được sản phẩm');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page, loaiSp]);

  const handleSearch = (e: FormEvent) => {
    e.preventDefault();
    setPage(0);
    load();
  };

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await createProduct({
        ...form,
        giaGiamGia: form.giaGiamGia || 0
      });
      setForm(EMPTY_FORM);
      setImageUrl('');
      setShowForm(false);
      setPage(0);
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Tạo sản phẩm thất bại');
    } finally {
      setSubmitting(false);
    }
  };

  const handleArchive = async (p: ProductSummary) => {
    if (!confirm(`Lưu trữ sản phẩm ${p.tenSp}?`)) return;
    try {
      await archiveProduct(p.id);
      await load();
    } catch (e: any) {
      setError(e?.response?.data?.message || 'Lưu trữ thất bại');
    }
  };

  const addImage = () => {
    if (!imageUrl.trim()) return;
    setForm({
      ...form,
      images: [
        ...(form.images || []),
        { imageUrl: imageUrl.trim(), sortOrder: (form.images?.length || 0) }
      ]
    });
    setImageUrl('');
  };

  const removeImage = (i: number) => {
    setForm({ ...form, images: form.images?.filter((_, idx) => idx !== i) });
  };

  const totalPages = Math.max(1, Math.ceil(total / size));

  return (
    <>
      <div className="admin-topbar">
        <h1>Sản phẩm</h1>
        <div className="d-flex gap-2">
          <button
            className="btn btn-primary btn-sm"
            onClick={() => setShowForm((v) => !v)}
          >
            {showForm ? 'Đóng form' : '+ Thêm sản phẩm'}
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
        <form className="row g-2 align-items-end" onSubmit={handleSearch}>
          <div className="col-md-4">
            <label className="form-label mb-1">Từ khoá</label>
            <input
              className="form-control form-control-sm"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Tên sản phẩm..."
            />
          </div>
          <div className="col-md-3">
            <label className="form-label mb-1">Loại</label>
            <select
              className="form-select form-select-sm"
              value={loaiSp}
              onChange={(e) => {
                setLoaiSp(e.target.value);
                setPage(0);
              }}
            >
              <option value="">Tất cả</option>
              {categories.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
          <div className="col-md-2">
            <button className="btn btn-outline-primary btn-sm w-100">Tìm</button>
          </div>
          <div className="col-md-3 text-end">
            <small className="text-muted">
              {total} sản phẩm · trang {page + 1}/{totalPages}
            </small>
          </div>
        </form>
      </div>

      {showForm && (
        <div className="admin-card mb-3">
          <h5 className="mb-3">Tạo sản phẩm mới</h5>
          <form className="row g-3" onSubmit={handleCreate}>
            <div className="col-md-6">
              <label className="form-label">Tên sản phẩm</label>
              <input
                className="form-control"
                value={form.tenSp}
                onChange={(e) => setForm({ ...form, tenSp: e.target.value })}
                required
                maxLength={300}
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Loại</label>
              <input
                className="form-control"
                value={form.loaiSp}
                onChange={(e) => setForm({ ...form, loaiSp: e.target.value })}
                required
                list="cat-list"
                maxLength={50}
              />
              <datalist id="cat-list">
                {categories.map((c) => (
                  <option key={c} value={c} />
                ))}
              </datalist>
            </div>
            <div className="col-md-3">
              <label className="form-label">Thương hiệu</label>
              <input
                className="form-control"
                value={form.thuongHieu || ''}
                onChange={(e) => setForm({ ...form, thuongHieu: e.target.value })}
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Giá ban đầu</label>
              <input
                type="number"
                className="form-control"
                value={form.giaBanDau}
                onChange={(e) => setForm({ ...form, giaBanDau: Number(e.target.value) })}
                min={0}
                required
              />
            </div>
            <div className="col-md-3">
              <label className="form-label">Giá giảm</label>
              <input
                type="number"
                className="form-control"
                value={form.giaGiamGia || 0}
                onChange={(e) => setForm({ ...form, giaGiamGia: Number(e.target.value) })}
                min={0}
              />
            </div>
            <div className="col-md-2">
              <label className="form-label">Trọng lượng</label>
              <input
                type="number"
                className="form-control"
                value={form.trongLuong || ''}
                onChange={(e) =>
                  setForm({
                    ...form,
                    trongLuong: e.target.value ? Number(e.target.value) : undefined
                  })
                }
                min={0}
                step={0.01}
              />
            </div>
            <div className="col-md-2">
              <label className="form-label">Hàm lượng</label>
              <input
                className="form-control"
                value={form.hamLuong || ''}
                onChange={(e) => setForm({ ...form, hamLuong: e.target.value })}
              />
            </div>
            <div className="col-md-2">
              <label className="form-label">Giới tính</label>
              <select
                className="form-select"
                value={form.gioiTinh || ''}
                onChange={(e) => setForm({ ...form, gioiTinh: e.target.value })}
              >
                <option value="">—</option>
                <option value="Nam">Nam</option>
                <option value="Nữ">Nữ</option>
                <option value="Unisex">Unisex</option>
              </select>
            </div>
            <div className="col-md-12">
              <label className="form-label">Mô tả</label>
              <textarea
                className="form-control"
                rows={3}
                value={form.description || ''}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
              />
            </div>
            <div className="col-md-12">
              <label className="form-label">Ảnh (URL)</label>
              <div className="d-flex gap-2 mb-2">
                <input
                  className="form-control"
                  value={imageUrl}
                  onChange={(e) => setImageUrl(e.target.value)}
                  placeholder="/products/abc.png hoặc https://..."
                />
                <button type="button" className="btn btn-outline-primary" onClick={addImage}>
                  Thêm
                </button>
              </div>
              {(form.images || []).map((img, i) => (
                <div key={i} className="d-flex align-items-center gap-2 mb-1">
                  <code className="text-truncate" style={{ flex: 1 }}>
                    {img.imageUrl}
                  </code>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-danger"
                    onClick={() => removeImage(i)}
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
            <div className="col-md-12">
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? 'Đang tạo...' : 'Tạo sản phẩm'}
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
                <th>Mã SP</th>
                <th>Tên</th>
                <th>Loại</th>
                <th className="text-end">Giá hiện tại</th>
                <th className="text-end">Giá gốc</th>
                <th>Trạng thái</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {items.length === 0 && (
                <tr>
                  <td colSpan={7} className="text-center text-muted py-4">
                    {loading ? 'Đang tải...' : 'Chưa có sản phẩm'}
                  </td>
                </tr>
              )}
              {items.map((p) => (
                <tr key={p.id}>
                  <td>
                    <code>{p.maSp}</code>
                  </td>
                  <td>
                    <div className="d-flex align-items-center gap-2">
                      {p.thumbnail && (
                        <img
                          src={p.thumbnail}
                          alt=""
                          style={{
                            width: 32,
                            height: 32,
                            objectFit: 'cover',
                            borderRadius: 4
                          }}
                        />
                      )}
                      <div>
                        <div>{p.tenSp}</div>
                        <small className="text-muted">{shortId(p.id, 8)}</small>
                      </div>
                    </div>
                  </td>
                  <td>{p.loaiSp}</td>
                  <td className="text-end">{fmtVnd(p.giaHienTai)}</td>
                  <td className="text-end text-muted">{fmtVnd(p.giaBanDau)}</td>
                  <td>
                    <span
                      className={`badge bg-${p.status === 'ACTIVE' ? 'success' : 'secondary'}`}
                    >
                      {p.status}
                    </span>
                  </td>
                  <td className="text-end">
                    {p.status === 'ACTIVE' && (
                      <button
                        className="btn btn-outline-danger btn-sm"
                        onClick={() => handleArchive(p)}
                      >
                        Lưu trữ
                      </button>
                    )}
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
