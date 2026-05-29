import { Container, Row, Col } from 'react-bootstrap';
import Link from 'next/link';

export const metadata = {
  title: 'Liên hệ — THLuxury'
};

export default function ContactPage() {
  return (
    <Container className="mt-5 pt-5 mb-5">
      <nav aria-label="breadcrumb" className="breadcrumb-wrapper">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <Link href="/">Trang chủ</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Liên hệ
          </li>
        </ol>
      </nav>
      <h2 className="fw-bold mb-4">Liên hệ THLuxury</h2>
      <Row className="g-4">
        <Col md={4}>
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h5 className="fw-bold">Chi nhánh TP.HCM</h5>
              <p className="text-muted mb-1">123 Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh</p>
              <p className="text-muted">028 1234 5678</p>
            </div>
          </div>
        </Col>
        <Col md={4}>
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h5 className="fw-bold">Chi nhánh Hà Nội</h5>
              <p className="text-muted mb-1">88 Tràng Tiền, Hoàn Kiếm, Hà Nội</p>
              <p className="text-muted">024 9876 5432</p>
            </div>
          </div>
        </Col>
        <Col md={4}>
          <div className="card border-0 shadow-sm h-100">
            <div className="card-body">
              <h5 className="fw-bold">Chi nhánh Đà Nẵng</h5>
              <p className="text-muted mb-1">66 Bạch Đằng, Hải Châu, Đà Nẵng</p>
              <p className="text-muted">0236 4567 890</p>
            </div>
          </div>
        </Col>
      </Row>
      <hr className="my-5" />
      <Row>
        <Col md={8}>
          <h4 className="fw-bold">Tổng đài CSKH</h4>
          <p>
            Email:{' '}
            <a href="mailto:support@thluxury.local" className="thluxury-link">
              support@thluxury.local
            </a>
          </p>
          <p>Hotline: 1800 5454 — Hỗ trợ 24/7</p>
        </Col>
      </Row>
    </Container>
  );
}
