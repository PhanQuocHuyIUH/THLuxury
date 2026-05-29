'use client';
import { useEffect } from 'react';
import { Container, Row, Col, Card, Button, Form } from 'react-bootstrap';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'framer-motion';
import { useCartStore } from '@/store/cart';
import { formatVnd } from '@/lib/format';

export default function CartPage() {
  const router = useRouter();
  const items = useCartStore((s) => s.items);
  const selectedIds = useCartStore((s) => s.selectedIds);
  const hydrated = useCartStore((s) => s.hydrated);
  const updateQuantity = useCartStore((s) => s.updateQuantity);
  const removeItem = useCartStore((s) => s.removeItem);
  const toggleSelect = useCartStore((s) => s.toggleSelect);
  const toggleSelectAll = useCartStore((s) => s.toggleSelectAll);
  const totalSelected = useCartStore((s) => s.totalSelected());

  useEffect(() => {
    if (hydrated && items.length > 0 && selectedIds.length === 0) {
      useCartStore.getState().toggleSelectAll(true);
    }
  }, [hydrated, items.length, selectedIds.length]);

  const handleCheckout = () => {
    if (selectedIds.length === 0) {
      alert('Vui lòng chọn ít nhất một sản phẩm để thanh toán.');
      return;
    }
    router.push('/checkout');
  };

  if (!hydrated) {
    return (
      <Container className="mt-5 pt-5 mb-5">
        <p className="text-center text-muted">Đang tải giỏ hàng...</p>
      </Container>
    );
  }

  if (items.length === 0) {
    return (
      <Container className="mt-5 pt-5 mb-5" style={{ minHeight: '50vh' }}>
        <nav aria-label="breadcrumb" className="breadcrumb-wrapper">
          <ol className="breadcrumb">
            <li className="breadcrumb-item">
              <Link href="/">Trang chủ</Link>
            </li>
            <li className="breadcrumb-item active" aria-current="page">
              Giỏ hàng
            </li>
          </ol>
        </nav>
        <div className="text-center">
          <h2 style={{ fontStyle: 'italic', color: '#555' }}>Giỏ hàng trống</h2>
          <Button className="btn-thluxury mt-3" onClick={() => router.push('/products')}>
            Quay lại mua sắm
          </Button>
        </div>
      </Container>
    );
  }

  return (
    <div style={{ backgroundColor: '#f5f5f5', minHeight: '100vh' }}>
      <Container className="pt-5">
        <nav aria-label="breadcrumb" className="breadcrumb-wrapper">
          <ol className="breadcrumb">
            <li className="breadcrumb-item">
              <Link href="/">Trang chủ</Link>
            </li>
            <li className="breadcrumb-item active" aria-current="page">
              Giỏ hàng
            </li>
          </ol>
        </nav>
        <h2 className="mb-4 fw-bold" style={{ borderBottom: '2px solid #f2e3bf', paddingBottom: 10 }}>
          Giỏ Hàng ({items.length} sản phẩm)
        </h2>
        <Row className="g-4 pb-5">
          <Col lg={8}>
            <Form.Check
              type="checkbox"
              label="Chọn tất cả"
              checked={selectedIds.length === items.length}
              onChange={(e) => toggleSelectAll(e.target.checked)}
              className="mb-3 fw-bold"
            />
            <AnimatePresence>
              {items.map((item) => (
                <motion.div
                  key={item.productId}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -20 }}
                  transition={{ duration: 0.3 }}
                >
                  <Card className="mb-3 border-0 shadow-sm" style={{ borderRadius: 15 }}>
                    <Card.Body>
                      <Row className="align-items-center">
                        <Col xs={1}>
                          <Form.Check
                            type="checkbox"
                            checked={selectedIds.includes(item.productId)}
                            onChange={() => toggleSelect(item.productId)}
                          />
                        </Col>
                        <Col xs={3} md={2}>
                          <Link href={`/products/${item.productId}`}>
                            <img
                              src={item.hinh}
                              alt={item.tenSp}
                              className="img-fluid rounded"
                              style={{ borderRadius: 10 }}
                            />
                          </Link>
                        </Col>
                        <Col xs={8} md={6}>
                          <h5 style={{ fontSize: '1.1rem' }}>{item.tenSp}</h5>
                          {item.thuongHieu && <p className="mb-1 text-muted">Thương hiệu: {item.thuongHieu}</p>}
                          {item.loaiDa && (
                            <p className="mb-1">
                              {item.loaiDa}
                              {item.mauDa ? ` - ${item.mauDa}` : ''}
                            </p>
                          )}
                          <p className="text-danger fw-bold mb-0">
                            {formatVnd(item.giaHienTai)} VND
                            {item.giaGiamGia && item.giaGiamGia > 0 && item.giaGiamGia < item.giaBanDau && (
                              <span className="text-muted ms-2 text-decoration-line-through">
                                {formatVnd(item.giaBanDau)} VND
                              </span>
                            )}
                          </p>
                        </Col>
                        <Col xs={12} md={3} className="mt-3 mt-md-0">
                          <div className="d-flex align-items-center justify-content-md-end">
                            <Button
                              variant="outline-secondary"
                              size="sm"
                              onClick={() => updateQuantity(item.productId, Math.max(1, item.quantity - 1))}
                              style={{ borderRadius: '50%', width: 32, height: 32 }}
                            >
                              -
                            </Button>
                            <Form.Control
                              type="number"
                              min={1}
                              value={item.quantity}
                              onChange={(e) =>
                                updateQuantity(item.productId, Math.max(1, parseInt(e.target.value) || 1))
                              }
                              className="mx-2 text-center"
                              style={{ width: 60 }}
                            />
                            <Button
                              variant="outline-secondary"
                              size="sm"
                              onClick={() => updateQuantity(item.productId, item.quantity + 1)}
                              style={{ borderRadius: '50%', width: 32, height: 32 }}
                            >
                              +
                            </Button>
                            <Button
                              variant="outline-danger"
                              size="sm"
                              className="ms-2"
                              onClick={() => removeItem(item.productId)}
                            >
                              Xóa
                            </Button>
                          </div>
                        </Col>
                      </Row>
                    </Card.Body>
                  </Card>
                </motion.div>
              ))}
            </AnimatePresence>
          </Col>
          <Col lg={4}>
            <Card className="border-0 shadow-sm" style={{ borderRadius: 15, position: 'sticky', top: 20 }}>
              <Card.Body>
                <div className="d-flex justify-content-between mb-3 pb-2" style={{ borderBottom: '1px solid #ddd' }}>
                  <span className="fw-bold">Tổng Tiền ({selectedIds.length} sản phẩm)</span>
                  <span className="fw-bold text-danger">{formatVnd(totalSelected)} VND</span>
                </div>
                <Button className="btn-thluxury w-100 mb-2" size="lg" onClick={handleCheckout}>
                  Thanh Toán
                </Button>
                <Button variant="outline-secondary" className="w-100" onClick={() => router.push('/products')}>
                  Tiếp tục mua sắm
                </Button>
              </Card.Body>
            </Card>
          </Col>
        </Row>
      </Container>
    </div>
  );
}
