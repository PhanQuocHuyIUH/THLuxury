'use client';
import { Container, Row, Col, Nav } from 'react-bootstrap';
import { FaFacebookF, FaInstagram, FaYoutube, FaEnvelope } from 'react-icons/fa';
import { motion } from 'framer-motion';

export default function Footer() {
  return (
    <footer
      style={{
        backgroundColor: '#f5f5f5',
        padding: '50px 0 30px',
        borderTop: '1px solid #ddd',
        fontSize: '0.9rem',
        color: '#333'
      }}
    >
      <Container>
        <h2 style={{ color: '#b8860b', fontWeight: 'bold', marginBottom: 20 }}>THLuxury</h2>
        <Row>
          <Col md={4} sm={6} className="mb-4">
            <p style={{ lineHeight: 1.6 }}>
              © 2026 THLuxury — Trang sức cao cấp
              <br />
              123 Nguyễn Huệ, Q.1, TP.HCM
            </p>
            <p style={{ lineHeight: 1.6 }}>
              ĐT: 028 1234 5678 — Tổng đài CSKH 24/7
              <br />
              Email:{' '}
              <a href="mailto:support@thluxury.local" style={{ color: '#003468' }}>
                support@thluxury.local
              </a>
            </p>
          </Col>

          <Col md={2} sm={6} className="mb-4">
            <h5 style={{ fontWeight: 'bold', marginBottom: 15, color: '#003468' }}>VỀ THLuxury</h5>
            <Nav className="flex-column">
              {['Tuyển dụng', 'Kinh doanh sỉ', 'Kiểm định kim cương', 'Tin tức'].map((item) => (
                <Nav.Link key={item} href="#" className="p-0 text-dark mb-2">
                  {item}
                </Nav.Link>
              ))}
            </Nav>
          </Col>

          <Col md={3} sm={6} className="mb-4">
            <h5 style={{ fontWeight: 'bold', marginBottom: 15, color: '#003468' }}>DỊCH VỤ KHÁCH HÀNG</h5>
            <Nav className="flex-column">
              {[
                'Hướng dẫn đo size trang sức',
                'Mua hàng trả góp',
                'Hướng dẫn thanh toán',
                'Cẩm nang sử dụng trang sức',
                'Câu hỏi thường gặp'
              ].map((item) => (
                <Nav.Link key={item} href="#" className="p-0 text-dark mb-2">
                  {item}
                </Nav.Link>
              ))}
            </Nav>
          </Col>

          <Col md={3} sm={6} className="mb-4">
            <h5 style={{ fontWeight: 'bold', marginBottom: 15, color: '#003468' }}>KẾT NỐI VỚI CHÚNG TÔI</h5>
            <div className="d-flex mb-3">
              <motion.a href="#" className="me-3" whileHover={{ scale: 1.2 }}>
                <FaFacebookF size={24} style={{ color: '#3b5998' }} />
              </motion.a>
              <motion.a href="#" className="me-3" whileHover={{ scale: 1.2 }}>
                <FaInstagram size={24} style={{ color: '#e1306c' }} />
              </motion.a>
              <motion.a href="#" className="me-3" whileHover={{ scale: 1.2 }}>
                <FaYoutube size={24} style={{ color: '#ff0000' }} />
              </motion.a>
              <motion.a href="mailto:support@thluxury.local" whileHover={{ scale: 1.2 }}>
                <FaEnvelope size={24} style={{ color: '#00acee' }} />
              </motion.a>
            </div>
            <h5 style={{ fontWeight: 'bold', marginBottom: 15, color: '#003468' }}>CHÍNH SÁCH</h5>
            <Nav className="flex-column">
              {[
                'Chính sách hoàn tiền',
                'Chính sách giao hàng',
                'Chính sách bảo hành',
                'Chính sách bảo mật'
              ].map((item) => (
                <Nav.Link key={item} href="#" className="p-0 text-dark mb-2">
                  {item}
                </Nav.Link>
              ))}
            </Nav>
          </Col>
        </Row>

        <Row className="mt-4">
          <Col className="text-center text-muted">
            <small>THLuxury © 2026 — Demo microservices project.</small>
          </Col>
        </Row>
      </Container>
    </footer>
  );
}
