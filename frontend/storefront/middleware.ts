import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// Auth state is in localStorage (zustand persist), accessible client-side only.
// We don't enforce auth server-side via cookie; client pages redirect when needed.
// This middleware is a no-op placeholder kept for future cookie-based auth.

export function middleware(_req: NextRequest) {
  return NextResponse.next();
}

export const config = {
  matcher: []
};
