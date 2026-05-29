'use client';
import { useEffect, useState } from 'react';
import { Container, Row, Col, Card, Badge, Button } from 'react-bootstrap';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import { useAuthStore } from '@/store/auth';
import { apiClient } from '@/lib/api';
import { formatVnd } from '@/lib/format';

const STATUS_LABEL: Record<string, string> = {
  CREATED: 'Đang xử lý',
  RESERVED: 'Đã giữ hàng',
  PRICED: 'Đã tính giá',
  PAID: 'Đã thanh toán',
  CONFIRMED: 'Đã xác nhận (thanh toán khi nhận)',
  PREPARING: 'Đang chuẩn bị',
  READY_FOR_PICKUP: 'Sẵn sàng giao',
  SHIPPING: 'Đang giao',
  DELIVERED: 'Đã giao',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
  FAILED: 'Thất bại'
};

export default function OrderDetailPage() {
  const params = useParams<{ maDh: string }>();
  const router = useRouter();
  const hydrated = useAuthStore((s) => s.hydrated);
  const user = useAuthStore((s) => s.user);
  const [order, setOrder] = useState<any | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!hydrated) return;
    if (!user) {
      router.push(`/login?next=/order/${params.maDh}`);
      return;
    }
    apiClient
      .get(`/api/orders/me/${params.maDh}`)
      .then((res) => setOrder(res.data))
      .catch((e) => setError(e?.response?.data?.message || 'Không tìm thấy đơn hàng'))
      .finally(() => setLoading(false));
  }, [hydrated, user, params.maDh, router]);

  if (!hydrated) {
    return (
      <Container className="mt-5 pt-5">
        <p className="text-center text-muted">Đang tải...</p>
      </Container>
    );
  }

  if (loading) {
    return (
      <Container className="mt-5 pt-5">
        <p className="text-center text-muted">Đang tải đơn hàng...</p>
      </Container>
    );
  }

  if (error || !order) {
    return (
      <Container className="mt-5 pt-5 text-center">
        <h4>Không tìm thấy đơn hàng</h4>
        <p className="text-muted">{error}</p>
        <Link href="/profile">
          <Button className="btn-thluxury mt-3">Về trang tài khoản</Button>
        </Link>
      </Container>
    );
  }

  const items: any[] = Array.isArray(order.itemsSnapshot)
    ? order.itemsSnapshot
    : typeof order.itemsSnapshot === 'string'
    ? JSON.parse(order.itemsSnapshot)
    : [];
  const customer = typeof order.customerSnapshot === 'string' ? JSON.parse(order.customerSnapshot) : order.customerSnapshot;
  const address = typeof order.addressSnapshot === 'string' ? JSON.parse(order.addressSnapshot) : order.addressSnapshot;

  return (
    <Container className="mt-5 pt-5 mb-5">
      <nav aria-label="breadcrumb" className="breadcrumb-wrapper">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <Link href="/">Trang chủ</Link>
          </li>
          <li className="breadcrumb-item">
            <Link href="/profile">Tài khoản</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Đơn hàng {order.maDh}
          </li>
        </ol>
      </nav>

      <h3 className="fw-bold mb-3">
        Đơn hàng {order.maDh}{' '}
        <Badge bg={['DELIVERED', 'COMPLETED'].includes(order.currentStatus) ? 'success' : 'primary'} className="ms-2">
          {STATUS_LABEL[order.currentStatus] || order.currentStatus}
        </Badge>
      </h3>

      <Row className="g-4 mt-2">
        <Col md={8}>
          <Card className="border-0 shadow-sm">
            <Card.Body>
              <h6 className="fw-bold mb-3">Sản phẩm</h6>
              {items.map((it: any, idx: number) => (
                <Row key={idx} className="mb-3 pb-2 border-bottom align-items-center">
                  <Col xs={2}>
                    {it.hinh && <img src={it.hinh} alt={it.tenSp} style={{ width: '100%', borderRadius: 6 }} />}
                  </Col>
                  <Col xs={6}>
                    <div className="fw-bold">{it.tenSp}</div>
                    <small className="text-muted">SL: {it.soLuong}</small>
                  </Col>
                  <Col xs={4} className="text-end">
                    {formatVnd(Number(it.giaMua) * Number(it.soLuong))} VND
                  </Col>
                </Row>
              ))}
            </Card.Body>
          </Card>

          {customer && (
            <Card className="border-0 shadow-sm mt-3">
              <Card.Body>
                <h6 className="fw-bold mb-2">Thông tin nhận hàng</h6>
                <p className="mb-1">
                  <strong>{customer.fullName}</strong> • {customer.phone}
                </p>
                <p className="mb-1 text-muted">{customer.email}</p>
                {address && (
                  <p className="mb-0">
                    {[address.diaChi, address.phuong, address.quan, address.thanhPho].filter(Boolean).join(', ')}
                  </p>
                )}
                {order.deliveryType === 'STORE_PICKUP' && (
                  <p className="mb-0 text-muted">Nhận tại cửa hàng</p>
                )}
              </Card.Body>
            </Card>
          )}
        </Col>

        <Col md={4}>
          <Card className="border-0 shadow-sm" style={{ position: 'sticky', top: 20 }}>
            <Card.Body>
              <h6 className="fw-bold mb-3">Tóm tắt</h6>
              <div className="d-flex justify-content-between mb-1">
                <span>Tạm tính</span>
                <span>{formatVnd(order.subtotal)} VND</span>
              </div>
              {Number(order.discountAmount || 0) > 0 && (
                <div className="d-flex justify-content-between mb-1">
                  <span>Giảm giá</span>
                  <span>-{formatVnd(order.discountAmount)} VND</span>
                </div>
              )}
              <div className="d-flex justify-content-between mb-1">
                <span>VAT</span>
                <span>{formatVnd(order.vatAmount)} VND</span>
              </div>
              <hr />
              <div className="d-flex justify-content-between fw-bold">
                <span>Tổng cộng</span>
                <span className="text-danger">{formatVnd(order.total)} VND</span>
              </div>
              {order.paymentMethod && (
                <p className="text-muted small mt-3 mb-0">Phương thức: {order.paymentMethod}</p>
              )}
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}
