'use client';
import { useEffect, useRef, useState } from 'react';
import { apiClient } from '@/lib/api';
import { applySession } from '@/lib/session';

const GSI_SRC = 'https://accounts.google.com/gsi/client';
const CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || '';

interface GoogleCredentialResponse {
  credential?: string;
}

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string;
            callback: (resp: GoogleCredentialResponse) => void;
          }) => void;
          renderButton: (parent: HTMLElement, options: Record<string, unknown>) => void;
        };
      };
    };
  }
}

function loadGsiScript(): Promise<void> {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined') return reject(new Error('no window'));
    if (window.google?.accounts?.id) return resolve();
    const existing = document.querySelector<HTMLScriptElement>(`script[src="${GSI_SRC}"]`);
    if (existing) {
      existing.addEventListener('load', () => resolve());
      existing.addEventListener('error', () => reject(new Error('gsi load failed')));
      return;
    }
    const script = document.createElement('script');
    script.src = GSI_SRC;
    script.async = true;
    script.defer = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error('gsi load failed'));
    document.head.appendChild(script);
  });
}

interface Props {
  /** Gọi sau khi đăng nhập Google thành công (vd: điều hướng). */
  onSuccess: () => void;
  onError?: (message: string) => void;
}

export default function GoogleSignInButton({ onSuccess, onError }: Props) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!CLIENT_ID) return; // chưa cấu hình → ẩn nút
    let cancelled = false;

    loadGsiScript()
      .then(() => {
        if (cancelled || !window.google || !containerRef.current) return;
        window.google.accounts.id.initialize({
          client_id: CLIENT_ID,
          callback: async (resp: GoogleCredentialResponse) => {
            if (!resp.credential) {
              onError?.('Không nhận được id_token từ Google');
              return;
            }
            try {
              const { data } = await apiClient.post('/api/auth/oauth2/google', {
                idToken: resp.credential
              });
              await applySession(data);
              onSuccess();
            } catch (err: any) {
              const msg = err?.response?.data?.message || 'Đăng nhập Google thất bại';
              onError?.(msg);
            }
          }
        });
        window.google.accounts.id.renderButton(containerRef.current, {
          theme: 'outline',
          size: 'large',
          width: 320,
          text: 'continue_with',
          locale: 'vi'
        });
        setReady(true);
      })
      .catch(() => onError?.('Không tải được Google Sign-In'));

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  if (!CLIENT_ID) return null;

  return (
    <div className="mt-3">
      <div className="d-flex align-items-center my-3" aria-hidden="true">
        <hr className="flex-grow-1" />
        <span className="px-2 text-muted small">hoặc</span>
        <hr className="flex-grow-1" />
      </div>
      <div className="d-flex justify-content-center" ref={containerRef} />
      {!ready && <div className="text-center text-muted small mt-1">Đang tải Google…</div>}
    </div>
  );
}
