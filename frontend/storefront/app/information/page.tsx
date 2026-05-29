import { Container, Image } from 'react-bootstrap';
import Link from 'next/link';

export const metadata = {
  title: 'Giới thiệu — THLuxury'
};

export default function InformationPage() {
  return (
    <Container className="mt-5 pt-5 mb-5">
      <nav aria-label="breadcrumb" className="breadcrumb-wrapper">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <Link href="/">Trang chủ</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Giới thiệu
          </li>
        </ol>
      </nav>
      <h2 className="fw-bold mb-4">Giới thiệu THLuxury</h2>
      <Image src="/assets/b1.jpg" fluid rounded className="mb-4" style={{ maxHeight: 360, objectFit: 'cover', width: '100%' }} />
      <p>
        THLuxury là thương hiệu trang sức cao cấp với hơn 100 mẫu trang sức kim cương, vàng 18K-24K, đá quý
        được thiết kế tỉ mỉ bởi đội ngũ thợ thủ công lành nghề. Sứ mệnh của chúng tôi là mang đến những sản phẩm
        đỉnh cao về chất lượng cùng dịch vụ khách hàng tận tâm.
      </p>
      <p>
        Hệ thống 3 chi nhánh tại Hồ Chí Minh, Hà Nội và Đà Nẵng giúp khách hàng có thể trải nghiệm và nhận tư
        vấn trực tiếp từ chuyên gia, hoặc đặt hàng online với chính sách giao toàn quốc.
      </p>
      <h4 className="fw-bold mt-4">Giá trị cốt lõi</h4>
      <ul>
        <li>Chất lượng — Mỗi sản phẩm đều được kiểm định trước khi đến tay khách hàng.</li>
        <li>Tận tâm — Đội ngũ CSKH 24/7 sẵn sàng hỗ trợ.</li>
        <li>Minh bạch — Cam kết về nguồn gốc, chất lượng và giá cả công khai.</li>
      </ul>
    </Container>
  );
}
