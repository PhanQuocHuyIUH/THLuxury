'use client';
import { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useAuthStore } from '@/store/auth';
import { API_URL } from '@/lib/api';
import type { ChatResponse, SuggestedProduct } from '@/types';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
  suggestedProducts?: SuggestedProduct[];
  suggestions?: string[];
}

interface Props {
  show: boolean;
  onClose: () => void;
}

function sanitizeInput(input: string): string {
  return input
    .replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
    .replace(/\s*on\w+\s*=\s*['"][^'"]*['"]/gi, '')
    .replace(/javascript:/gi, '')
    .trim();
}

function genSessionId(): string {
  return 'sess-' + Math.random().toString(36).slice(2, 12) + Date.now().toString(36);
}

export default function ChatbotModal({ show, onClose }: Props) {
  const user = useAuthStore((s) => s.user);
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: 'assistant',
      content:
        'Chào bạn! Tôi là trợ lý ảo của THLuxury. Bạn cần hỗ trợ gì về trang sức hôm nay?',
      suggestions: [
        'Giá nhẫn vàng 18K là bao nhiêu?',
        'Chính sách giao hàng của THLuxury?',
        'Có nhẫn kim cương nào không?'
      ]
    }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const sessionRef = useRef<string>(genSessionId());
  const endRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const send = async (text: string) => {
    const clean = sanitizeInput(text);
    if (!clean) return;
    setMessages((prev) => [...prev, { role: 'user', content: clean }]);
    setInput('');
    setLoading(true);

    try {
      const res = await fetch(`${API_URL}/api/ai/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          sessionId: sessionRef.current,
          userId: user?.id || null,
          message: clean
        })
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data: ChatResponse = await res.json();
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: data.reply,
          suggestedProducts: data.suggestedProducts
        }
      ]);
    } catch (e) {
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: 'Xin lỗi, hệ thống trợ lý ảo đang bận. Vui lòng thử lại sau.'
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleSuggestion = (s: string) => {
    setInput(s);
    void send(s);
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      void send(input);
    }
  };

  if (!show) return null;

  const userInitial = user?.fullName?.charAt(0)?.toUpperCase() || 'B';

  return (
    <div
      style={{
        position: 'fixed',
        bottom: 100,
        right: 20,
        width: 420,
        maxWidth: 'calc(100vw - 40px)',
        height: 600,
        maxHeight: 'calc(100vh - 140px)',
        backgroundColor: '#fff',
        borderRadius: 15,
        boxShadow: '0 4px 20px rgba(0,0,0,0.25)',
        zIndex: 1000,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden'
      }}
    >
      <div
        style={{
          backgroundColor: '#f2e3bf',
          color: '#003468',
          padding: '12px 16px',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        <strong>Trợ lý ảo THLuxury</strong>
        <button
          onClick={onClose}
          style={{ background: 'none', border: 'none', color: '#003468', fontSize: '1.2rem', cursor: 'pointer' }}
          aria-label="Đóng"
        >
          ✕
        </button>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: 16 }}>
        {messages.map((m, i) => (
          <div
            key={i}
            style={{
              display: 'flex',
              flexDirection: m.role === 'user' ? 'row-reverse' : 'row',
              alignItems: 'flex-start',
              marginBottom: 12
            }}
          >
            <div
              style={{
                width: 36,
                height: 36,
                borderRadius: '50%',
                backgroundColor: m.role === 'user' ? '#003468' : '#f2e3bf',
                color: m.role === 'user' ? '#fff' : '#003468',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 'bold',
                margin: m.role === 'user' ? '0 0 0 8px' : '0 8px 0 0'
              }}
            >
              {m.role === 'user' ? userInitial : 'T'}
            </div>
            <div
              style={{
                maxWidth: '75%',
                padding: '10px 14px',
                borderRadius: 14,
                backgroundColor: m.role === 'user' ? '#f2e3bf' : '#f5f5f5',
                color: '#333',
                fontSize: '0.95rem',
                whiteSpace: 'pre-wrap'
              }}
            >
              {m.content}
              {m.suggestions && (
                <div style={{ marginTop: 8, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
                  {m.suggestions.map((s) => (
                    <button
                      key={s}
                      onClick={() => handleSuggestion(s)}
                      style={{
                        backgroundColor: '#fff',
                        color: '#003468',
                        border: '1px solid #003468',
                        borderRadius: 12,
                        padding: '4px 10px',
                        fontSize: '0.8rem',
                        cursor: 'pointer'
                      }}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              )}
              {m.suggestedProducts && m.suggestedProducts.length > 0 && (
                <div style={{ marginTop: 10 }}>
                  {m.suggestedProducts.map((p) => (
                    <Link
                      key={p.productId}
                      href={`/products/${p.productId}`}
                      onClick={onClose}
                      style={{
                        display: 'flex',
                        gap: 8,
                        alignItems: 'center',
                        marginBottom: 6,
                        textDecoration: 'none',
                        color: '#333'
                      }}
                    >
                      <img
                        src={p.imageUrl}
                        alt={p.tenSP}
                        style={{ width: 44, height: 44, objectFit: 'cover', borderRadius: 6 }}
                      />
                      <div>
                        <div style={{ fontWeight: 'bold', fontSize: '0.85rem' }}>{p.tenSP}</div>
                        <div style={{ color: '#003468', fontSize: '0.8rem' }}>
                          {p.giaSP.toLocaleString('vi-VN')} VND
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              )}
            </div>
          </div>
        ))}
        {loading && (
          <div style={{ color: '#777', fontStyle: 'italic', fontSize: '0.85rem' }}>Đang trả lời...</div>
        )}
        <div ref={endRef} />
      </div>

      <div
        style={{
          padding: 12,
          borderTop: '1px solid #eee',
          display: 'flex',
          alignItems: 'center',
          gap: 8
        }}
      >
        <textarea
          rows={2}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Nhập câu hỏi..."
          disabled={loading}
          style={{
            resize: 'none',
            flex: 1,
            border: '1px solid #ddd',
            borderRadius: 10,
            padding: 8,
            fontSize: '0.9rem'
          }}
        />
        <button
          onClick={() => send(input)}
          disabled={loading || !input.trim()}
          className="btn-thluxury"
          style={{
            padding: '8px 16px',
            border: 'none',
            cursor: loading || !input.trim() ? 'not-allowed' : 'pointer'
          }}
        >
          Gửi
        </button>
      </div>
    </div>
  );
}
