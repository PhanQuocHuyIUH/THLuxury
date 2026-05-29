'use client';
import { Row, Col } from 'react-bootstrap';
import ProductCard from './ProductCard';
import type { Product } from '@/types';

interface Props {
  products: Product[];
  emptyText?: string;
}

export default function ListProduct({ products, emptyText = 'Không có sản phẩm nào' }: Props) {
  if (!products || products.length === 0) {
    return (
      <div className="text-center py-5">
        <h5 className="text-muted">{emptyText}</h5>
      </div>
    );
  }
  return (
    <Row className="g-4">
      {products.map((p) => (
        <Col key={p.id} xs={12} sm={6} md={4} lg={3}>
          <ProductCard product={p} />
        </Col>
      ))}
    </Row>
  );
}
