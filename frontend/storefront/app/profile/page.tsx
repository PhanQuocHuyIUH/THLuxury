'use client';
import { useEffect, useState } from 'react';
import { Container, Row, Col, Card, Button, Tab, Tabs, Table, Badge } from 'react-bootstrap';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useAuthStore } from '@/store/auth';
import { useCartStore } from '@/store/cart';
import { apiClient } from '@/lib/api';
import { formatVnd } from '@/lib/format';

interface OrderRow {
  id: string;
  maDh: string;
  currentStatus: string;
  total: number;
  createdAt: string;
}

const STATUS_LABEL: Record<string, string> = {
  CREATED: 'Đang xử lý',
  RESERVED: 'Đã giữ hàng',
  PRICED: 'Đã tính giá',
  PAID: 'Đã thanh toán',
  CONFIRMED: 'Đã xác nhận (COD)',
  PREPARING: 'Đang chuẩn bị',
  READY_FOR_PICKUP: 'Sẵn sàng giao',
  SHIPPING: 'Đang giao',
  DELIVERED: 'Đã giao',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
  FAILED: 'Thất bại'
};

const STATUS_VARIANT: Record<string, string> = {
  CREATED: 'secondary',
  PAID: 'primary',
  CONFIRMED: 'warning',
  PREPARING: 'info',
  SHIPPING: 'info',
  DELIVERED: 'success',
  COMPLETED: 'success',
  CANCELLED: 'danger',
  FAILED: 'danger'
};

export default function ProfilePage() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const hydrated = useAuthStore((s) => s.hydrated);
  const [orders, setOrders] = useState<OrderRow[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!hydrated) return;
    if (!user) {
      router.push('/login?next=/profile');
      return;
    }
    apiClient
      .get('/api/orders/me?size=20')
      .then((res) => {
        const data = res.data;
        const items: any[] = Array.isArray(data) ? data : data.content || [];
        setOrders(
          items.map((o: any) => ({
            id: o.id,
            maDh: o.maDh,
            currentStatus: o.currentStatus,
            total: Number(o.total || 0),
            createdAt: o.createdAt
          }))
        );
      })
      .catch(() => setOrders([]))
      .finally(() => setLoading(false));
  }, [hydrated, user, router]);

  const handleLogout = async () => {
    try {
      const refreshToken = useAuthStore.getState().refreshToken;
      if (refreshToken) {
        await apiClient.post('/api/auth/logout', { refreshToken });
      }
    } catch {
      // ignore
    } finally {
      useAuthStore.getState().clear();
      useCartStore.getState().clearAll();
      router.push('/');
    }
  };

  if (!hydrated || !user) {
    return (
      <Container className="mt-5 pt-5">
        <p className="text-center text-muted">Đang tải...</p>
      </Container>
    );
  }

  return (
    <Container className="mt-5 pt-5 mb-5">
      <Row>
        <Col md={4}>
          <Card className="border-0 shadow-sm mb-4">
            <Card.Body>
              <h5 className="fw-bold">{user.fullName}</h5>
              <p className="text-muted mb-1">{user.email}</p>
              <Badge bg="info" className="mb-3">
                {user.role}
              </Badge>
              <Button variant="outline-danger" size="sm" className="w-100" onClick={handleLogout}>
                Đăng xuất
              </Button>
            </Card.Body>
          </Card>
        </Col>
        <Col md={8}>
          <Tabs defaultActiveKey="orders" className="mb-3">
            <Tab eventKey="orders" title="Đơn hàng của tôi">
              {loading ? (
                <p className="text-muted">Đang tải đơn hàng...</p>
              ) : orders.length === 0 ? (
                <p className="text-muted">Bạn chưa có đơn hàng nào.</p>
              ) : (
                <Table responsive hover>
                  <thead>
                    <tr>
                      <th>Mã ĐH</th>
                      <th>Ngày đặt</th>
                      <th>Tổng tiền</th>
                      <th>Trạng thái</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    {orders.map((o) => (
                      <tr key={o.id}>
                        <td className="fw-bold">{o.maDh}</td>
                        <td>{new Date(o.createdAt).toLocaleDateString('vi-VN')}</td>
                        <td>{formatVnd(o.total)} VND</td>
                        <td>
                          <Badge bg={STATUS_VARIANT[o.currentStatus] || 'secondary'}>
                            {STATUS_LABEL[o.currentStatus] || o.currentStatus}
                          </Badge>
                        </td>
                        <td>
                          <Link href={`/order/${o.maDh}`}>Chi tiết</Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </Tab>
            <Tab eventKey="info" title="Thông tin tài khoản">
              <Card className="border-0 shadow-sm">
                <Card.Body>
                  <p>
                    <strong>Họ tên:</strong> {user.fullName}
                  </p>
                  <p>
                    <strong>Email:</strong> {user.email}
                  </p>
                  <p>
                    <strong>Vai trò:</strong> {user.role}
                  </p>
                </Card.Body>
              </Card>
            </Tab>
          </Tabs>
        </Col>
      </Row>
    </Container>
  );
}
