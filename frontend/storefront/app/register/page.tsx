'use client';
import { useState, FormEvent } from 'react';
import { Container, Row, Col, Form, Button } from 'react-bootstrap';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { apiClient } from '@/lib/api';
import GoogleSignInButton from '@/components/GoogleSignInButton';

export default function RegisterPage() {
  const router = useRouter();
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', password: '', confirm: '' });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (form.password !== form.confirm) {
      setError('Mật khẩu nhập lại không khớp.');
      return;
    }
    setLoading(true);
    try {
      await apiClient.post('/api/auth/register', {
        email: form.email,
        password: form.password,
        fullName: form.fullName,
        phone: form.phone
      });
      router.push('/login');
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Đăng ký thất bại';
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
            <h2 className="text-center fw-bold mb-4">Đăng ký tài khoản</h2>
            {error && <div className="alert alert-danger">{error}</div>}
            <Form onSubmit={handleSubmit}>
              <Form.Group className="mb-3">
                <Form.Label>Họ và tên</Form.Label>
                <Form.Control
                  value={form.fullName}
                  onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                  required
                />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Email</Form.Label>
                <Form.Control
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  required
                />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Số điện thoại</Form.Label>
                <Form.Control
                  value={form.phone}
                  onChange={(e) => setForm({ ...form, phone: e.target.value })}
                />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Mật khẩu</Form.Label>
                <Form.Control
                  type="password"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  required
                  minLength={6}
                />
              </Form.Group>
              <Form.Group className="mb-3">
                <Form.Label>Nhập lại mật khẩu</Form.Label>
                <Form.Control
                  type="password"
                  value={form.confirm}
                  onChange={(e) => setForm({ ...form, confirm: e.target.value })}
                  required
                  minLength={6}
                />
              </Form.Group>
              <Button type="submit" className="btn-thluxury w-100" disabled={loading}>
                {loading ? 'Đang tạo tài khoản...' : 'Đăng ký'}
              </Button>

              <GoogleSignInButton
                onSuccess={() => router.push('/')}
                onError={(msg) => setError(msg)}
              />

              <div className="text-center mt-3">
                <Link href="/login" style={{ color: '#003468' }}>
                  Đã có tài khoản? Đăng nhập
                </Link>
              </div>
            </Form>
          </motion.div>
        </Col>
      </Row>
    </Container>
  );
}
