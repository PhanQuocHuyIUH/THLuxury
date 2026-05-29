'use client';
import { Row, Col, Card, Button, Image } from 'react-bootstrap';
import Carousel from 'react-multi-carousel';
import Link from 'next/link';
import type { Product } from '@/types';
import { formatVnd, productThumb, effectivePrice } from '@/lib/format';

interface Props {
  products: Product[];
}

const categories = [
  { img: '/assets/d1.jpg', title: 'Nhẫn', desc: 'Tinh tế & Sang trọng' },
  { img: '/assets/d2.jpg', title: 'Dây Chuyền', desc: 'Điểm nhấn nổi bật' },
  { img: '/assets/d3.jpg', title: 'Vòng Tay', desc: 'Thời thượng & Cá tính' },
  { img: '/assets/d4.jpg', title: 'Lắc Tay', desc: 'Thanh lịch mỗi ngày' }
];

export default function HomeContent({ products }: Props) {
  return (
    <>
      <div className="mb-5 rounded overflow-hidden shadow">
        <Image
          src="/assets/bg.jpg"
          alt="Banner"
          fluid
          style={{ borderRadius: 20, maxHeight: 400, objectFit: 'cover', width: '100%' }}
        />
      </div>

      <div
        style={{
          background: '#fffde7',
          borderRadius: 20,
          padding: '30px 20px',
          boxShadow: '0 4px 20px rgba(0,0,0,0.08)'
        }}
        className="mb-5"
      >
        <h2 className="text-center fw-bold mb-4 text-uppercase" style={{ color: '#a9a9a9' }}>
          Danh mục nổi bật
        </h2>
        <Row>
          {categories.map((item) => (
            <Col xs={6} md={3} key={item.title} className="mb-4">
              <Card className="shadow-sm border-0 product-card">
                <Card.Img variant="top" src={item.img} style={{ height: 200, objectFit: 'cover' }} />
                <Card.Body>
                  <Card.Title className="text-center fw-bold">{item.title}</Card.Title>
                  <Card.Text className="text-center text-muted" style={{ fontSize: '0.85rem' }}>
                    {item.desc}
                  </Card.Text>
                </Card.Body>
              </Card>
            </Col>
          ))}
        </Row>
      </div>

      <h2 className="text-center mb-4 fw-bold text-uppercase">Bộ Sưu Tập Trang Sức</h2>
      {products && products.length > 0 ? (
        <Carousel
          arrows
          autoPlay
          autoPlaySpeed={3500}
          containerClass="carousel-container"
          infinite
          itemClass="px-2"
          keyBoardControl
          pauseOnHover
          responsive={{
            superLargeDesktop: { breakpoint: { max: 4000, min: 1200 }, items: 4 },
            desktop: { breakpoint: { max: 1200, min: 992 }, items: 3 },
            tablet: { breakpoint: { max: 992, min: 768 }, items: 2 },
            mobile: { breakpoint: { max: 768, min: 0 }, items: 1 }
          }}
          swipeable
        >
          {products.slice(0, 12).map((p) => {
            const thumb = productThumb(p.images);
            const price = effectivePrice(p);
            return (
              <Card key={p.id} className="h-100 shadow-sm border-0 product-card">
                <Card.Img variant="top" src={thumb} alt={p.tenSp} style={{ height: 220, objectFit: 'cover' }} />
                <Card.Body className="d-flex flex-column">
                  <Card.Title className="text-truncate" style={{ fontWeight: 'bold' }}>
                    {p.tenSp}
                  </Card.Title>
                  <small className="text-muted mb-2">
                    {p.loaiSp} • {p.thuongHieu}
                  </small>
                  <div className="mb-2">
                    {p.giaGiamGia && p.giaGiamGia > 0 && p.giaGiamGia < p.giaBanDau ? (
                      <>
                        <span className="text-muted text-decoration-line-through me-2">
                          {formatVnd(p.giaBanDau)} VNĐ
                        </span>
                        <span className="text-danger fw-bold">{formatVnd(price)} VNĐ</span>
                      </>
                    ) : (
                      <span className="fw-bold">{formatVnd(price)} VNĐ</span>
                    )}
                  </div>
                  <div className="mt-auto">
                    <Link href={`/products/${p.id}`}>
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
          })}
        </Carousel>
      ) : (
        <div className="text-center text-muted">
          <p>Hiện chưa có sản phẩm để hiển thị.</p>
        </div>
      )}
    </>
  );
}
