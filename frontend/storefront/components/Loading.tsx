'use client';
import { Spinner } from 'react-bootstrap';

export default function Loading({ text = 'Đang tải...' }: { text?: string }) {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center py-5" style={{ minHeight: 240 }}>
      <Spinner animation="border" role="status" style={{ color: '#b8860b' }} />
      <p className="mt-3 text-muted">{text}</p>
    </div>
  );
}
