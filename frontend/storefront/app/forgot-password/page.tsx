'use client';
import { useState, FormEvent } from 'react';
import { Container, Row, Col, Form, Button } from 'react-bootstrap';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { apiClient } from '@/lib/api';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await apiClient.post('/api/auth/password/forgot', { email });
      setSent(true);
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Gửi yêu cầu thất bại, vui lòng thử lại.';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container className="mt-5 pt-5 mb-5">
      <Row className="justify-content-center">
        <Col md={8} lg={6}>
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            style={{
              backgroundColor: '#fff',
              padding: '2rem',
              borderRadius: 15,
              boxShadow: '0 6px 25px rgba(0,0,0,0.08)'
            }}
          >
            <h2 className="text-center fw-bold mb-3">Quên mật khẩu</h2>
            {sent ? (
              <>
                <div className="alert alert-success">
                  Nếu email tồn tại trong hệ thống, chúng tôi đã gửi liên kết đặt lại mật khẩu.
                  Vui lòng kiểm tra hộp thư của bạn.
                </div>
                <div className="text-center mt-3">
                  <Link href="/login" style={{ color: '#003468' }}>
                    Quay lại đăng nhập
                  </Link>
                </div>
              </>
            ) : (
              <>
                <p className="text-muted text-center mb-4">
                  Nhập email tài khoản của bạn, chúng tôi sẽ gửi liên kết đặt lại mật khẩu.
                </p>
                {error && <div className="alert alert-danger">{error}</div>}
                <Form onSubmit={handleSubmit}>
                  <Form.Group className="mb-3">
                    <Form.Label>Email</Form.Label>
                    <Form.Control
                      type="email"
                      value={email}
                      onChange={(e) => setEmail(e.target.value)}
                      required
                      style={{ borderRadius: 10 }}
                    />
                  </Form.Group>
                  <Button type="submit" className="btn-thluxury w-100" disabled={loading}>
                    {loading ? 'Đang gửi...' : 'Gửi liên kết đặt lại'}
                  </Button>
                  <div className="text-center mt-3">
                    <Link href="/login" style={{ color: '#003468' }}>
                      Quay lại đăng nhập
                    </Link>
                  </div>
                </Form>
              </>
            )}
          </motion.div>
        </Col>
      </Row>
    </Container>
  );
}
