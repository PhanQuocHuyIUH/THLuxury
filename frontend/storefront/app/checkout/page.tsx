'use client';
import { useEffect, useState } from 'react';
import { Container, Row, Col, Card, Button, Form, ProgressBar } from 'react-bootstrap';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useCartStore } from '@/store/cart';
import { useAuthStore } from '@/store/auth';
import { apiClient, API_URL } from '@/lib/api';
import { formatVnd } from '@/lib/format';
import type { Branch } from '@/types';

type Step = 1 | 2 | 3;
type DeliveryType = 'STORE_PICKUP' | 'HOME_DELIVERY';

export default function CheckoutPage() {
  const router = useRouter();
  const hydrated = useCartStore((s) => s.hydrated);
  const selectedItems = useCartStore((s) => s.selectedItems());
  const authHydrated = useAuthStore((s) => s.hydrated);
  const user = useAuthStore((s) => s.user);

  const [step, setStep] = useState<Step>(1);
  const [branches, setBranches] = useState<Branch[]>([]);
  const [loadingBranches, setLoadingBranches] = useState(true);

  const [deliveryType, setDeliveryType] = useState<DeliveryType>('STORE_PICKUP');
  const [branchId, setBranchId] = useState<string>('');
  const [address, setAddress] = useState({ diaChi: '', thanhPho: '', quan: '', phuong: '' });
  const [customer, setCustomer] = useState({ fullName: '', phone: '', email: '' });
  const [voucherCode, setVoucherCode] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<'COD' | 'BANK_TRANSFER' | 'CREDIT_CARD'>('COD');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authHydrated) return;
    if (!user) {
      router.push('/login?next=/checkout');
    }
  }, [authHydrated, user, router]);

  useEffect(() => {
    if (user) {
      setCustomer({
        fullName: user.fullName || '',
        phone: user.phone || '',
        email: user.email || ''
      });
    }
  }, [user]);

  useEffect(() => {
    let cancelled = false;
    fetch(`${API_URL}/api/branches`)
      .then((r) => (r.ok ? r.json() : []))
      .then((d) => {
        if (cancelled) return;
        if (Array.isArray(d)) {
          setBranches(
            d.map((b: any) => ({
              id: String(b.id),
              code: b.code,
              name: b.name,
              address: b.address,
              city: b.city,
              district: b.district,
              ward: b.ward,
              phone: b.phone
            }))
          );
          if (d.length > 0) setBranchId(String(d[0].id));
        }
      })
      .catch(() => setBranches([]))
      .finally(() => !cancelled && setLoadingBranches(false));
    return () => {
      cancelled = true;
    };
  }, []);

  const subtotal = selectedItems.reduce((s, i) => s + i.giaHienTai * i.quantity, 0);
  const vatPct = 10;
  const vatAmount = Math.round((subtotal * vatPct) / 100);
  const total = subtotal + vatAmount;

  if (!hydrated || !authHydrated) {
    return (
      <Container className="mt-5 pt-5">
        <p className="text-center text-muted">Đang tải...</p>
      </Container>
    );
  }

  if (selectedItems.length === 0) {
    return (
      <Container className="mt-5 pt-5 text-center">
        <h3>Không có sản phẩm nào để thanh toán</h3>
        <p className="text-muted">Vui lòng chọn sản phẩm trong giỏ hàng trước.</p>
        <Button className="btn-thluxury mt-3" onClick={() => router.push('/cart')}>
          Quay lại giỏ hàng
        </Button>
      </Container>
    );
  }

  const validateStep1 = (): string | null => {
    if (deliveryType === 'STORE_PICKUP' && !branchId) return 'Vui lòng chọn chi nhánh nhận hàng.';
    if (deliveryType === 'HOME_DELIVERY' && (!address.diaChi || !address.thanhPho)) {
      return 'Vui lòng nhập đầy đủ địa chỉ giao hàng.';
    }
    if (!customer.fullName || !customer.phone) return 'Vui lòng nhập họ tên và số điện thoại.';
    return null;
  };

  const handleNext = () => {
    setError(null);
    if (step === 1) {
      const err = validateStep1();
      if (err) {
        setError(err);
        return;
      }
      setStep(2);
    } else if (step === 2) {
      setStep(3);
    }
  };

  const handleSubmit = async () => {
    setError(null);
    setSubmitting(true);
    try {
      const cartPayload = selectedItems.map((i) => ({ productId: i.productId, quantity: i.quantity }));
      await apiClient.post('/api/cart/sync', { items: cartPayload });

      const checkoutBody: any = {
        deliveryType,
        paymentMethod,
        customer,
        voucherCode: voucherCode || null
      };
      if (deliveryType === 'STORE_PICKUP') {
        checkoutBody.branchId = branchId;
      } else {
        checkoutBody.address = address;
        if (branchId) checkoutBody.branchId = branchId;
      }

      const { data } = await apiClient.post('/api/orders/checkout', checkoutBody);
      const maDh = data?.maDh;

      useCartStore.getState().clearSelected();

      if (maDh) {
        router.push(`/order/${maDh}`);
      } else {
        router.push('/profile');
      }
    } catch (e: any) {
      const msg = e?.response?.data?.message || e?.message || 'Thanh toán thất bại, vui lòng thử lại.';
      setError(msg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Container className="mt-5 pt-5 mb-5">
      <nav aria-label="breadcrumb" className="breadcrumb-wrapper">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <Link href="/">Trang chủ</Link>
          </li>
          <li className="breadcrumb-item">
            <Link href="/cart">Giỏ hàng</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Thanh toán
          </li>
        </ol>
      </nav>

      <h2 className="fw-bold mb-3">Thanh toán</h2>
      <ProgressBar now={(step / 3) * 100} className="mb-4" style={{ height: 8 }} />
      <div className="d-flex justify-content-between mb-4" style={{ fontSize: '0.9rem' }}>
        <span className={step >= 1 ? 'fw-bold' : 'text-muted'}>1. Thông tin nhận hàng</span>
        <span className={step >= 2 ? 'fw-bold' : 'text-muted'}>2. Kiểm tra đơn</span>
        <span className={step >= 3 ? 'fw-bold' : 'text-muted'}>3. Thanh toán</span>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <Row className="g-4">
        <Col lg={8}>
          {step === 1 && (
            <Card className="border-0 shadow-sm">
              <Card.Body>
                <h5 className="fw-bold mb-3">Phương thức nhận hàng</h5>
                <Form.Check
                  type="radio"
                  id="d-pickup"
                  label="Nhận tại cửa hàng"
                  name="deliveryType"
                  checked={deliveryType === 'STORE_PICKUP'}
                  onChange={() => setDeliveryType('STORE_PICKUP')}
                />
                <Form.Check
                  type="radio"
                  id="d-home"
                  label="Giao hàng tận nơi"
                  name="deliveryType"
                  checked={deliveryType === 'HOME_DELIVERY'}
                  onChange={() => setDeliveryType('HOME_DELIVERY')}
                  className="mb-3"
                />

                {deliveryType === 'STORE_PICKUP' && (
                  <Form.Group className="mb-3">
                    <Form.Label>Chi nhánh nhận hàng</Form.Label>
                    {loadingBranches ? (
                      <div className="text-muted">Đang tải danh sách chi nhánh...</div>
                    ) : (
                      <Form.Select value={branchId} onChange={(e) => setBranchId(e.target.value)}>
                        {branches.map((b) => (
                          <option key={b.id} value={b.id}>
                            {b.name} {b.city ? `- ${b.city}` : ''}
                          </option>
                        ))}
                      </Form.Select>
                    )}
                  </Form.Group>
                )}

                {deliveryType === 'HOME_DELIVERY' && (
                  <>
                    <Row className="g-3 mb-3">
                      <Col md={6}>
                        <Form.Group>
                          <Form.Label>Địa chỉ</Form.Label>
                          <Form.Control
                            value={address.diaChi}
                            onChange={(e) => setAddress({ ...address, diaChi: e.target.value })}
                          />
                        </Form.Group>
                      </Col>
                      <Col md={6}>
                        <Form.Group>
                          <Form.Label>Tỉnh / Thành phố</Form.Label>
                          <Form.Control
                            value={address.thanhPho}
                            onChange={(e) => setAddress({ ...address, thanhPho: e.target.value })}
                          />
                        </Form.Group>
                      </Col>
                      <Col md={6}>
                        <Form.Group>
                          <Form.Label>Quận / Huyện</Form.Label>
                          <Form.Control
                            value={address.quan}
                            onChange={(e) => setAddress({ ...address, quan: e.target.value })}
                          />
                        </Form.Group>
                      </Col>
                      <Col md={6}>
                        <Form.Group>
                          <Form.Label>Phường / Xã</Form.Label>
                          <Form.Control
                            value={address.phuong}
                            onChange={(e) => setAddress({ ...address, phuong: e.target.value })}
                          />
                        </Form.Group>
                      </Col>
                    </Row>
                    <Form.Group className="mb-3">
                      <Form.Label>Chi nhánh xuất kho (tùy chọn)</Form.Label>
                      <Form.Select value={branchId} onChange={(e) => setBranchId(e.target.value)}>
                        <option value="">Để hệ thống tự chọn</option>
                        {branches.map((b) => (
                          <option key={b.id} value={b.id}>
                            {b.name} {b.city ? `- ${b.city}` : ''}
                          </option>
                        ))}
                      </Form.Select>
                    </Form.Group>
                  </>
                )}

                <hr />
                <h6 className="fw-bold mb-3">Thông tin liên hệ</h6>
                <Row className="g-3">
                  <Col md={6}>
                    <Form.Group>
                      <Form.Label>Họ và tên</Form.Label>
                      <Form.Control
                        value={customer.fullName}
                        onChange={(e) => setCustomer({ ...customer, fullName: e.target.value })}
                      />
                    </Form.Group>
                  </Col>
                  <Col md={6}>
                    <Form.Group>
                      <Form.Label>Số điện thoại</Form.Label>
                      <Form.Control
                        value={customer.phone}
                        onChange={(e) => setCustomer({ ...customer, phone: e.target.value })}
                      />
                    </Form.Group>
                  </Col>
                  <Col md={12}>
                    <Form.Group>
                      <Form.Label>Email</Form.Label>
                      <Form.Control
                        type="email"
                        value={customer.email}
                        onChange={(e) => setCustomer({ ...customer, email: e.target.value })}
                      />
                    </Form.Group>
                  </Col>
                </Row>
              </Card.Body>
            </Card>
          )}

          {step === 2 && (
            <Card className="border-0 shadow-sm">
              <Card.Body>
                <h5 className="fw-bold mb-3">Sản phẩm</h5>
                {selectedItems.map((it) => (
                  <Row key={it.productId} className="align-items-center mb-2 pb-2 border-bottom">
                    <Col xs={2}>
                      <img src={it.hinh} alt={it.tenSp} style={{ width: '100%', borderRadius: 6 }} />
                    </Col>
                    <Col xs={6}>
                      <div className="fw-bold">{it.tenSp}</div>
                      <small className="text-muted">SL: {it.quantity}</small>
                    </Col>
                    <Col xs={4} className="text-end">
                      {formatVnd(it.giaHienTai * it.quantity)} VND
                    </Col>
                  </Row>
                ))}

                <Form.Group className="mt-3">
                  <Form.Label>Mã giảm giá</Form.Label>
                  <Form.Control
                    placeholder="VD: WELCOME10"
                    value={voucherCode}
                    onChange={(e) => setVoucherCode(e.target.value.toUpperCase())}
                  />
                  <Form.Text className="text-muted">
                    Voucher sẽ được áp dụng và xác thực khi xác nhận đơn.
                  </Form.Text>
                </Form.Group>
              </Card.Body>
            </Card>
          )}

          {step === 3 && (
            <Card className="border-0 shadow-sm">
              <Card.Body>
                <h5 className="fw-bold mb-3">Phương thức thanh toán</h5>
                <Form.Check
                  type="radio"
                  id="pm-cod"
                  label="Thanh toán khi nhận hàng (COD)"
                  name="paymentMethod"
                  checked={paymentMethod === 'COD'}
                  onChange={() => setPaymentMethod('COD')}
                />
                <Form.Check
                  type="radio"
                  id="pm-bank"
                  label="Chuyển khoản ngân hàng"
                  name="paymentMethod"
                  checked={paymentMethod === 'BANK_TRANSFER'}
                  onChange={() => setPaymentMethod('BANK_TRANSFER')}
                />
                <Form.Check
                  type="radio"
                  id="pm-card"
                  label="Thẻ tín dụng / Stripe sandbox"
                  name="paymentMethod"
                  checked={paymentMethod === 'CREDIT_CARD'}
                  onChange={() => setPaymentMethod('CREDIT_CARD')}
                />
              </Card.Body>
            </Card>
          )}
        </Col>

        <Col lg={4}>
          <Card className="border-0 shadow-sm" style={{ position: 'sticky', top: 20 }}>
            <Card.Body>
              <h6 className="fw-bold mb-3">Tóm tắt đơn hàng</h6>
              <div className="d-flex justify-content-between mb-1">
                <span>Tạm tính ({selectedItems.length} SP)</span>
                <span>{formatVnd(subtotal)} VND</span>
              </div>
              <div className="d-flex justify-content-between mb-1">
                <span>VAT ({vatPct}%)</span>
                <span>{formatVnd(vatAmount)} VND</span>
              </div>
              <hr />
              <div className="d-flex justify-content-between fw-bold mb-3">
                <span>Tổng cộng</span>
                <span className="text-danger">{formatVnd(total)} VND</span>
              </div>

              <div className="d-flex gap-2">
                {step > 1 && (
                  <Button variant="outline-secondary" onClick={() => setStep((s) => (s - 1) as Step)}>
                    Quay lại
                  </Button>
                )}
                {step < 3 && (
                  <Button className="btn-thluxury flex-grow-1" onClick={handleNext}>
                    Tiếp tục
                  </Button>
                )}
                {step === 3 && (
                  <Button
                    className="btn-thluxury flex-grow-1"
                    onClick={handleSubmit}
                    disabled={submitting}
                  >
                    {submitting ? 'Đang xử lý...' : 'Đặt hàng'}
                  </Button>
                )}
              </div>
            </Card.Body>
          </Card>
        </Col>
      </Row>
    </Container>
  );
}
