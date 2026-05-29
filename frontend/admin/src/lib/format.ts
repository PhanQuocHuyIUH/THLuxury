export function fmtVnd(n: number | null | undefined): string {
  if (n == null) return '—';
  return n.toLocaleString('vi-VN') + ' ₫';
}

export function fmtDateTime(s: string | null | undefined): string {
  if (!s) return '—';
  try {
    return new Date(s).toLocaleString('vi-VN');
  } catch {
    return s;
  }
}

export function shortId(id: string | null | undefined, n = 8): string {
  if (!id) return '—';
  return id.length > n ? id.slice(0, n) + '…' : id;
}
