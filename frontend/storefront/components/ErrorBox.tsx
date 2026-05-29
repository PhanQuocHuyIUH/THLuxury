'use client';
import { Alert, Container } from 'react-bootstrap';

export default function ErrorBox({ message }: { message: string }) {
  return (
    <Container className="my-5">
      <Alert variant="danger">
        <Alert.Heading>Đã có lỗi xảy ra</Alert.Heading>
        <p>{message}</p>
      </Alert>
    </Container>
  );
}
