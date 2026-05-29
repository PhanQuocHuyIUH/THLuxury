'use client';
import { Suspense, useState, FormEvent } from 'react';
import { Container, Row, Col, Form, Button } from 'react-bootstrap';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { apiClient } from '@/lib/api';
import { applySession } from '@/lib/session';
import GoogleSignInButton from '@/components/GoogleSignInButton';

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const next = searchParams.get('next') || '/';
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const { data } = await apiClient.post('/api/auth/login', { email, password });
      await applySession(data);
      router.push(next);
    } catch (err: any) {
      const msg = err?.response?.data?.message || 'Đăng nhập thất bại';
      setError(msg);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        position: 'relative',
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        overflow: 'hidden',
        paddingTop: 80
      }}
    >
      <video
        src="/assets/dia_small.mp4"
        autoPlay
        loop
        muted
        playsInline
        style={{
          position: 'absolute',
          inset: 0,
          width: '100%',
          height: '100%',
          objectFit: 'cover',
          filter: 'blur(5px)',
          zIndex: -1
        }}
      />
      <div
        style={{
          position: 'absolute',
          inset: 0,
          background: 'linear-gradient(135deg, rgba(0,0,0,0.5), rgba(242,227,191,0.3))',
          zIndex: -1
        }}
      />
      <Container>
        <Row className="justify-content-center">
          <Col md={8} lg={6}>
            <motion.div
              initial={{ opacity: 0, y: 40 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.7 }}
              style={{
                backgroundColor: 'rgba(242,227,191,0.92)',
                padding: '2rem',
                borderRadius: 15,
                boxShadow: '0 8px 30px rgba(0,0,0,0.2)'
              }}
            >
              <h2 className="text-center fw-bold mb-4">Đăng nhập</h2>
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
                <Form.Group className="mb-3">
                  <Form.Label>Mật khẩu</Form.Label>
                  <Form.Control
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    style={{ borderRadius: 10 }}
                  />
                </Form.Group>
                <Button type="submit" className="btn-thluxury w-100" disabled={loading}>
                  {loading ? 'Đang đăng nhập...' : 'Đăng nhập'}
                </Button>
                <div className="text-center mt-2">
                  <Link href="/forgot-password" style={{ color: '#003468' }}>
                    Quên mật khẩu?
                  </Link>
                </div>

                <GoogleSignInButton
                  onSuccess={() => router.push(next)}
                  onError={(msg) => setError(msg)}
                />

                <div className="text-center mt-3">
                  <Link href="/register" style={{ color: '#003468' }}>
                    Chưa có tài khoản? Đăng ký ngay
                  </Link>
                </div>
              </Form>
            </motion.div>
          </Col>
        </Row>
      </Container>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div style={{ minHeight: '100vh' }} />}>
      <LoginForm />
    </Suspense>
  );
}
