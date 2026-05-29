'use client';
import Link from 'next/link';
import { Card, Button } from 'react-bootstrap';
import type { Product } from '@/types';
import { formatVnd, productThumb, effectivePrice } from '@/lib/format';

interface Props {
  product: Product;
}

export default function ProductCard({ product }: Props) {
  const thumb = productThumb(product.images);
  const price = effectivePrice(product);
  const hasDiscount = product.giaGiamGia && product.giaGiamGia > 0 && product.giaGiamGia < product.giaBanDau;

  return (
    <Card
      className="h-100 shadow-sm border-0 product-card"
      style={{ borderRadius: 20 }}
    >
      <Card.Img
        variant="top"
        src={thumb}
        alt={product.tenSp}
        style={{ height: 220, objectFit: 'cover' }}
      />
      <Card.Body className="d-flex flex-column">
        <Card.Title className="text-truncate" style={{ fontWeight: 'bold', fontSize: '1.05rem' }}>
          {product.tenSp}
        </Card.Title>
        <small className="text-muted mb-2">
          {product.loaiSp}
          {product.thuongHieu ? ` • ${product.thuongHieu}` : ''}
        </small>
        <div className="mb-2">
          {hasDiscount ? (
            <>
              <span className="text-muted text-decoration-line-through me-2">
                {formatVnd(product.giaBanDau)} VNĐ
              </span>
              <span className="text-danger fw-bold">{formatVnd(price)} VNĐ</span>
            </>
          ) : (
            <span className="fw-bold">{formatVnd(price)} VNĐ</span>
          )}
        </div>
        <small className="mb-3">
          {product.loaiDa && (
            <>
              <strong>Đá:</strong> {product.loaiDa}
              {product.mauDa ? ` (${product.mauDa})` : ''} <br />
            </>
          )}
          {product.gioiTinh && (
            <>
              <strong>Giới tính:</strong> {product.gioiTinh}
            </>
          )}
        </small>
        <div className="mt-auto">
          <Link href={`/products/${product.id}`}>
            <Button
              variant="outline-primary"
              size="sm"
              className="w-100"
              style={{
                borderRadius: 20,
                padding: 8,
                fontWeight: 'bold',
                color: '#333',
                borderColor: '#f2e3bf',
                backgroundColor: '#fff'
              }}
            >
              Xem chi tiết
            </Button>
          </Link>
        </div>
      </Card.Body>
    </Card>
  );
}
