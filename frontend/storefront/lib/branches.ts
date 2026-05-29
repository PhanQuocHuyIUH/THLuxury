import { API_URL } from '@/lib/api';
import type { Branch } from '@/types';

export async function fetchBranches(opts: { next?: { revalidate?: number } } = { next: { revalidate: 3600 } }): Promise<Branch[]> {
  try {
    const res = await fetch(`${API_URL}/api/branches`, opts);
    if (!res.ok) return [];
    const data = await res.json();
    if (Array.isArray(data)) {
      return data.map((b: any) => ({
        id: String(b.id),
        code: b.code,
        name: b.name,
        address: b.address,
        city: b.city,
        district: b.district,
        ward: b.ward,
        phone: b.phone
      }));
    }
    return [];
  } catch {
    return [];
  }
}
