import { Container } from 'react-bootstrap';
import Link from 'next/link';
import { notFound } from 'next/navigation';
import { fetchProductById, fetchProducts } from '@/lib/products';
import ProductDetailContent from './ProductDetailContent';
import ListProduct from '@/components/ListProduct';

export const revalidate = 86400;

export default async function ProductDetailPage({ params }: { params: { id: string } }) {
  const product = await fetchProductById(params.id);
  if (!product) return notFound();

  let similar: any[] = [];
  try {
    const res = await fetchProducts({ loaiSp: product.loaiSp, size: 8 });
    similar = res.content.filter((p) => p.id !== product.id).slice(0, 4);
  } catch {
    similar = [];
  }

  return (
    <Container className="mt-5 pt-4 mb-5">
      <nav aria-label="breadcrumb" className="breadcrumb-wrapper">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <Link href="/">Trang chủ</Link>
          </li>
          <li className="breadcrumb-item">
            <Link href="/products">Sản phẩm</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            {product.tenSp}
          </li>
        </ol>
      </nav>

      <ProductDetailContent product={product} />

      {similar.length > 0 && (
        <section className="mt-5">
          <h4 className="fw-bold mb-3">Sản phẩm tương tự</h4>
          <ListProduct products={similar} />
        </section>
      )}
    </Container>
  );
}
