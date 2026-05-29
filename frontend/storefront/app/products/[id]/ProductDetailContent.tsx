'use client';
import { useState } from 'react';
import { Row, Col, Button, Form } from 'react-bootstrap';
import { useRouter } from 'next/navigation';
import type { Product } from '@/types';
import { formatVnd, productThumb, effectivePrice } from '@/lib/format';
import { useCartStore } from '@/store/cart';

export default function ProductDetailContent({ product }: { product: Product }) {
  const router = useRouter();
  const addItem = useCartStore((s) => s.addItem);
  const [qty, setQty] = useState(1);
  const [activeImg, setActiveImg] = useState(productThumb(product.images));

  const price = effectivePrice(product);
  const hasDiscount = product.giaGiamGia && product.giaGiamGia > 0 && product.giaGiamGia < product.giaBanDau;
  const discountPct = hasDiscount
    ? Math.round((1 - product.giaGiamGia / product.giaBanDau) * 100)
    : 0;

  const handleAddToCart = () => {
    addItem(
      {
        productId: product.id,
        maSp: product.maSp,
        tenSp: product.tenSp,
        hinh: productThumb(product.images),
        giaHienTai: price,
        giaBanDau: product.giaBanDau,
        giaGiamGia: product.giaGiamGia,
        thuongHieu: product.thuongHieu,
        loaiDa: product.loaiDa,
        mauDa: product.mauDa,
        hamLuong: product.hamLuong,
        trongLuong: product.trongLuong
      },
      qty
    );
    router.push('/cart');
  };

  const handleBuyNow = () => {
    addItem(
      {
        productId: product.id,
        maSp: product.maSp,
        tenSp: product.tenSp,
        hinh: productThumb(product.images),
        giaHienTai: price,
        giaBanDau: product.giaBanDau,
        giaGiamGia: product.giaGiamGia,
        thuongHieu: product.thuongHieu,
        loaiDa: product.loaiDa,
        mauDa: product.mauDa,
        hamLuong: product.hamLuong,
        trongLuong: product.trongLuong
      },
      qty
    );
    useCartStore.getState().setSelected([product.id]);
    router.push('/checkout');
  };

  return (
    <Row className="g-4">
      <Col md={6}>
        <div
          style={{
            backgroundColor: '#fff',
            borderRadius: 15,
            padding: 20,
            boxShadow: '0 2px 10px rgba(0,0,0,0.06)'
          }}
        >
          <img
            src={activeImg}
            alt={product.tenSp}
            style={{ width: '100%', borderRadius: 12, objectFit: 'cover', maxHeight: 500 }}
          />
          {product.images.length > 1 && (
            <div className="d-flex mt-3" style={{ gap: 10, overflowX: 'auto' }}>
              {product.images.map((img) => (
                <img
                  key={img}
                  src={img}
                  alt=""
                  onClick={() => setActiveImg(img)}
                  style={{
                    width: 80,
                    height: 80,
                    objectFit: 'cover',
                    borderRadius: 8,
                    cursor: 'pointer',
                    border: img === activeImg ? '2px solid #f2e3bf' : '2px solid transparent'
                  }}
                />
              ))}
            </div>
          )}
        </div>
      </Col>

      <Col md={6}>
        <h3 className="fw-bold">{product.tenSp}</h3>
        <p className="text-muted mb-2">
          Mã SP: {product.maSp} • {product.thuongHieu}
        </p>
        <div className="mb-3">
          {hasDiscount ? (
            <div>
              <span className="text-muted text-decoration-line-through me-2">
                {formatVnd(product.giaBanDau)} VNĐ
              </span>
              <span className="text-danger fw-bold fs-4">{formatVnd(price)} VNĐ</span>
              <span className="ms-2 badge bg-danger">-{discountPct}%</span>
            </div>
          ) : (
            <span className="fw-bold fs-4">{formatVnd(price)} VNĐ</span>
          )}
        </div>

        <table className="table table-borderless">
          <tbody>
            <tr>
              <td className="text-muted">Loại sản phẩm</td>
              <td>{product.loaiSp}</td>
            </tr>
            <tr>
              <td className="text-muted">Loại đá</td>
              <td>
                {product.loaiDa} {product.mauDa && `(${product.mauDa})`}
              </td>
            </tr>
            <tr>
              <td className="text-muted">Hàm lượng</td>
              <td>{product.hamLuong}</td>
            </tr>
            <tr>
              <td className="text-muted">Trọng lượng</td>
              <td>{product.trongLuong}g</td>
            </tr>
            <tr>
              <td className="text-muted">Giới tính</td>
              <td>{product.gioiTinh}</td>
            </tr>
          </tbody>
        </table>

        {product.description && <p className="text-muted">{product.description}</p>}

        <div className="d-flex align-items-center mb-3">
          <span className="me-3">Số lượng:</span>
          <Button variant="outline-secondary" size="sm" onClick={() => setQty(Math.max(1, qty - 1))}>
            -
          </Button>
          <Form.Control
            type="number"
            min={1}
            value={qty}
            onChange={(e) => setQty(Math.max(1, parseInt(e.target.value) || 1))}
            className="mx-2 text-center"
            style={{ width: 70 }}
          />
          <Button variant="outline-secondary" size="sm" onClick={() => setQty(qty + 1)}>
            +
          </Button>
        </div>

        <div className="d-flex gap-2">
          <Button className="btn-thluxury flex-grow-1" onClick={handleAddToCart}>
            Thêm vào giỏ
          </Button>
          <Button variant="danger" className="flex-grow-1" onClick={handleBuyNow}>
            Mua ngay
          </Button>
        </div>
      </Col>
    </Row>
  );
}
