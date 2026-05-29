'use client';
import { Row, Col, Form, Pagination } from 'react-bootstrap';
import Link from 'next/link';
import ListProduct from '@/components/ListProduct';
import type { Product } from '@/types';

interface Props {
  products: Product[];
  total: number;
  page: number;
  size: number;
  categories: string[];
  searchParams: {
    keyword?: string;
    loaiSp?: string;
    mauDa?: string;
    gioiTinh?: string;
    giaMin?: string;
    giaMax?: string;
    sort?: string;
    page?: string;
  };
}

export default function ProductsView({ products, total, page, size, categories, searchParams }: Props) {
  const totalPages = Math.max(1, Math.ceil(total / size));

  const baseParams = new URLSearchParams();
  for (const [k, v] of Object.entries(searchParams)) {
    if (v && k !== 'page') baseParams.append(k, String(v));
  }
  const linkFor = (p: number) => {
    const u = new URLSearchParams(baseParams);
    u.set('page', String(p));
    return `/products?${u.toString()}`;
  };

  return (
    <Row>
      <Col md={3}>
        <Form method="get" action="/products">
          <h6 className="fw-bold mb-3">Bộ lọc</h6>
          <Form.Group className="mb-3" controlId="filter-keyword">
            <Form.Label>Từ khóa</Form.Label>
            <Form.Control name="keyword" defaultValue={searchParams.keyword || ''} />
          </Form.Group>
          <Form.Group className="mb-3" controlId="filter-loaiSp">
            <Form.Label>Loại sản phẩm</Form.Label>
            <Form.Select name="loaiSp" defaultValue={searchParams.loaiSp || ''}>
              <option value="">Tất cả</option>
              {categories.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </Form.Select>
          </Form.Group>
          <Form.Group className="mb-3" controlId="filter-gioiTinh">
            <Form.Label>Giới tính</Form.Label>
            <Form.Select name="gioiTinh" defaultValue={searchParams.gioiTinh || ''}>
              <option value="">Tất cả</option>
              <option value="Nam">Nam</option>
              <option value="Nữ">Nữ</option>
              <option value="Unisex">Unisex</option>
            </Form.Select>
          </Form.Group>
          <Form.Group className="mb-3" controlId="filter-gia">
            <Form.Label>Giá từ</Form.Label>
            <Form.Control type="number" name="giaMin" defaultValue={searchParams.giaMin || ''} />
            <Form.Label className="mt-2">Đến</Form.Label>
            <Form.Control type="number" name="giaMax" defaultValue={searchParams.giaMax || ''} />
          </Form.Group>
          <Form.Group className="mb-3" controlId="filter-sort">
            <Form.Label>Sắp xếp</Form.Label>
            <Form.Select name="sort" defaultValue={searchParams.sort || ''}>
              <option value="">Mới nhất</option>
              <option value="price-asc">Giá tăng dần</option>
              <option value="price-desc">Giá giảm dần</option>
            </Form.Select>
          </Form.Group>
          <button type="submit" className="btn btn-thluxury w-100">
            Áp dụng
          </button>
        </Form>
      </Col>
      <Col md={9}>
        <p className="text-muted">Tổng {total} sản phẩm</p>
        <ListProduct products={products} />
        {totalPages > 1 && (
          <Pagination className="justify-content-center mt-4">
            <Pagination.Prev as={Link as any} href={linkFor(Math.max(0, page - 1))} disabled={page === 0} />
            {Array.from({ length: Math.min(totalPages, 8) }, (_, i) => (
              <Pagination.Item key={i} as={Link as any} href={linkFor(i)} active={i === page}>
                {i + 1}
              </Pagination.Item>
            ))}
            <Pagination.Next
              as={Link as any}
              href={linkFor(Math.min(totalPages - 1, page + 1))}
              disabled={page >= totalPages - 1}
            />
          </Pagination>
        )}
      </Col>
    </Row>
  );
}
