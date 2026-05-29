import { Container } from 'react-bootstrap';
import HomeContent from './HomeContent';
import { fetchProducts } from '@/lib/products';

export const dynamic = 'force-dynamic';

export default async function HomePage() {
  let products: any[] = [];
  try {
    const res = await fetchProducts({ size: 12, sort: 'newest' });
    products = res.content;
  } catch (e) {
    console.error('Failed to load products on Home:', e);
  }
  return (
    <Container className="mt-4 mb-5">
      <HomeContent products={products} />
    </Container>
  );
}
