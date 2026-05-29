'use client';
import { Suspense, useState, FormEvent } from 'react';
import { Container, Row, Col, Form, Button } from 'react-bootstrap';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { apiClient } from '@/lib/api';

function ResetPasswordForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const token = searchParams.get('token') || '';

  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!token) {
      setError('Liên kết không hợp lệ hoặc đã hết hạn.');
      return;
    }
    if (password.length < 8) {
      setError('Mật khẩu phải có ít nhất 8 ký tự.');
      return;
    }
    if (password !== confirm) {
      setError('Mật khẩu nhập lại không khớp.');
      return;
    }
    setLoading(true);
    try {
      await apiClient.post('/api/auth/password/reset', { token, password });
      setDone(true);
      setTimeout(() => router.push('/login'), 2500);
    } catch (err: any) {
      const msg =
        err?.response?.data?.message || 'Đặt lại mật khẩu thất bại, vui lòng thử lại.';
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
            <h2 className="text-center fw-bold mb-3">Đặt lại mật khẩu</h2>
            {done ? (
              <div className="alert alert-success text-center">
                Mật khẩu đã được cập nhật. Đang chuyển tới trang đăng nhập...
              </div>
            ) : !token ? (
              <>
                <div className="alert alert-danger">
                  Liên kết không hợp lệ hoặc thiếu mã xác thực.
                </div>
                <div className="text-center mt-3">
                  <Link href="/forgot-password" style={{ color: '#003468' }}>
                    Yêu cầu liên kết mới
                  </Link>
                </div>
              </>
            ) : (
              <>
                {error && <div className="alert alert-danger">{error}</div>}
                <Form onSubmit={handleSubmit}>
                  <Form.Group className="mb-3">
                    <Form.Label>Mật khẩu mới</Form.Label>
                    <Form.Control
                      type="password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      required
                      minLength={8}
                      style={{ borderRadius: 10 }}
                    />
                  </Form.Group>
                  <Form.Group className="mb-3">
                    <Form.Label>Nhập lại mật khẩu mới</Form.Label>
                    <Form.Control
                      type="password"
                      value={confirm}
                      onChange={(e) => setConfirm(e.target.value)}
                      required
                      minLength={8}
                      style={{ borderRadius: 10 }}
                    />
                  </Form.Group>
                  <Button type="submit" className="btn-thluxury w-100" disabled={loading}>
                    {loading ? 'Đang cập nhật...' : 'Đặt lại mật khẩu'}
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

export default function ResetPasswordPage() {
  return (
    <Suspense fallback={<Container className="mt-5 pt-5 mb-5 text-center">Đang tải...</Container>}>
      <ResetPasswordForm />
    </Suspense>
  );
}
