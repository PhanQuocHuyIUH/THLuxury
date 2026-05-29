import { Container } from 'react-bootstrap';
import Link from 'next/link';
import { fetchProducts, fetchCategories } from '@/lib/products';
import ProductsView from './ProductsView';

interface SearchParams {
  keyword?: string;
  loaiSp?: string;
  mauDa?: string;
  gioiTinh?: string;
  giaMin?: string;
  giaMax?: string;
  sort?: string;
  page?: string;
}

export const dynamic = 'force-dynamic';

export default async function ProductsPage({ searchParams }: { searchParams: SearchParams }) {
  const page = Number(searchParams.page || 0);
  const size = 12;
  const params = {
    keyword: searchParams.keyword,
    loaiSp: searchParams.loaiSp,
    mauDa: searchParams.mauDa,
    gioiTinh: searchParams.gioiTinh,
    giaMin: searchParams.giaMin,
    giaMax: searchParams.giaMax,
    sort: searchParams.sort,
    page,
    size
  };

  let products: any[] = [];
  let total = 0;
  try {
    const res = await fetchProducts(params, { cache: 'no-store' });
    products = res.content;
    total = res.total;
  } catch (e) {
    console.error(e);
  }
  const categories = await fetchCategories();

  return (
    <Container className="mt-5 pt-4 mb-5">
      <nav aria-label="breadcrumb" className="breadcrumb-wrapper">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <Link href="/">Trang chủ</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Sản phẩm
          </li>
        </ol>
      </nav>

      <h2 className="mb-4 fw-bold">Danh sách sản phẩm</h2>

      <ProductsView
        products={products}
        total={total}
        page={page}
        size={size}
        categories={categories}
        searchParams={searchParams}
      />
    </Container>
  );
}
