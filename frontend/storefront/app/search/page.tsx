import { Container } from 'react-bootstrap';
import Link from 'next/link';
import { fetchProducts } from '@/lib/products';
import ListProduct from '@/components/ListProduct';

export const dynamic = 'force-dynamic';

interface Props {
  searchParams: { query?: string };
}

export default async function SearchPage({ searchParams }: Props) {
  const keyword = searchParams.query || '';
  let products: any[] = [];
  let total = 0;
  if (keyword) {
    try {
      const res = await fetchProducts({ keyword, size: 24 }, { cache: 'no-store' });
      products = res.content;
      total = res.total;
    } catch {
      products = [];
    }
  }

  return (
    <Container className="mt-5 pt-5 mb-5">
      <nav aria-label="breadcrumb" className="breadcrumb-wrapper">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <Link href="/">Trang chủ</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Tìm kiếm
          </li>
        </ol>
      </nav>
      <h3 className="fw-bold mb-3">Kết quả cho &quot;{keyword}&quot;</h3>
      <p className="text-muted">Tìm thấy {total} sản phẩm</p>
      <ListProduct products={products} emptyText="Không tìm thấy sản phẩm phù hợp" />
    </Container>
  );
}
