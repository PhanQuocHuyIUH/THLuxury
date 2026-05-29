export function formatVnd(value: number | string | null | undefined): string {
  if (value === null || value === undefined) return '0';
  const num = typeof value === 'string' ? parseFloat(value) : value;
  if (Number.isNaN(num)) return '0';
  return num.toLocaleString('vi-VN');
}

export function productThumb(images: string[] | undefined, fallback = '/assets/img1.jpg'): string {
  if (images && images.length > 0) {
    const first: any = images[0];
    if (typeof first === 'string') return first;
    if (first && typeof first === 'object' && 'imageUrl' in first) return (first as any).imageUrl as string;
  }
  return fallback;
}

export function effectivePrice(p: { giaHienTai?: number; giaGiamGia?: number; giaBanDau: number }): number {
  if (typeof p.giaHienTai === 'number' && p.giaHienTai > 0) return p.giaHienTai;
  if (p.giaGiamGia && p.giaGiamGia > 0) return p.giaGiamGia;
  return p.giaBanDau;
}
