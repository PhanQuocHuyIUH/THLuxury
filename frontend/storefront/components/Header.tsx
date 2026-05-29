'use client';
import { useEffect, useState } from 'react';
import { usePathname, useRouter } from 'next/navigation';
import Link from 'next/link';
import { Navbar, Nav, Container, Button, Form } from 'react-bootstrap';
import { FaShoppingCart } from 'react-icons/fa';
import { motion } from 'framer-motion';
import { useCartStore } from '@/store/cart';
import { useAuthStore } from '@/store/auth';

export default function Header() {
  const [isScrolled, setIsScrolled] = useState(false);
  const [searchText, setSearchText] = useState('');
  const pathname = usePathname();
  const router = useRouter();
  const isHomePage = pathname === '/';

  const totalItems = useCartStore((s) => s.totalItems());
  const cartHydrated = useCartStore((s) => s.hydrated);
  const user = useAuthStore((s) => s.user);
  const authHydrated = useAuthStore((s) => s.hydrated);

  useEffect(() => {
    const handleScroll = () => {
      const viewport = typeof window !== 'undefined' ? window.innerHeight : 800;
      setIsScrolled(window.scrollY > viewport - 60);
    };
    handleScroll();
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      const q = searchText.trim();
      if (!q) return;
      router.push(`/search?query=${encodeURIComponent(q)}`);
      setSearchText('');
    }
  };

  const heroVariants = {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.8, delay: 0.2 } }
  };

  const lightText = isHomePage && !isScrolled;

  return (
    <header
      style={{
        position: 'relative',
        height: isHomePage ? '100vh' : 'auto',
        width: '100%',
        marginBottom: isHomePage ? 0 : 60
      }}
    >
      <Navbar
        variant="light"
        expand="md"
        fixed="top"
        className="px-4"
        style={{
          backgroundColor: lightText ? 'transparent' : '#fff',
          boxShadow: lightText ? 'none' : '0 4px 15px rgba(0,0,0,0.08)',
          transition: 'all 0.3s ease'
        }}
      >
        <Container fluid>
          <Navbar.Brand
            as={Link as any}
            href="/"
            className={lightText ? 'text-white' : 'text-dark'}
            style={{ fontWeight: 'bold', fontSize: '1.5rem' }}
          >
            THLuxury
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="storefrontNavbar" />
          <Navbar.Collapse id="storefrontNavbar">
            <Nav className="me-auto">
              {[
                { href: '/', label: 'Trang chủ' },
                { href: '/information', label: 'Giới thiệu' },
                { href: '/products', label: 'Sản phẩm' },
                { href: '/contact', label: 'Liên hệ' }
              ].map((item) => (
                <Nav.Link
                  key={item.href}
                  as={Link as any}
                  href={item.href}
                  className={lightText ? 'text-white' : 'text-dark'}
                  style={{ fontWeight: 500, padding: '10px 15px' }}
                >
                  {item.label}
                </Nav.Link>
              ))}
            </Nav>
            <Nav className="align-items-center">
              <Form style={{ position: 'relative', marginRight: 15 }}>
                <input
                  type="text"
                  placeholder="Tìm kiếm..."
                  value={searchText}
                  onChange={(e) => setSearchText(e.target.value)}
                  onKeyDown={handleKeyDown}
                  style={{
                    backgroundColor: lightText ? 'rgba(255,255,255,0.2)' : '#f5f5f5',
                    border: '1px solid #ccc',
                    color: lightText ? '#fff' : '#333',
                    width: 240,
                    height: 36,
                    borderRadius: 20,
                    paddingLeft: 36,
                    outline: 'none'
                  }}
                />
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  style={{
                    position: 'absolute',
                    top: '50%',
                    left: 10,
                    transform: 'translateY(-50%)',
                    width: 18,
                    height: 18,
                    color: lightText ? '#fff' : '#333',
                    pointerEvents: 'none'
                  }}
                >
                  <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
                </svg>
              </Form>

              <Nav.Link
                as={Link as any}
                href="/cart"
                className={lightText ? 'text-white' : 'text-dark'}
                style={{ position: 'relative', marginRight: 15 }}
              >
                <FaShoppingCart style={{ width: 24, height: 24 }} />
                {cartHydrated && totalItems > 0 && (
                  <span
                    style={{
                      position: 'absolute',
                      top: -5,
                      right: -10,
                      backgroundColor: '#ad2a36',
                      color: '#fff',
                      borderRadius: '50%',
                      padding: '2px 6px',
                      fontSize: '0.75rem',
                      fontWeight: 'bold'
                    }}
                  >
                    {totalItems}
                  </span>
                )}
              </Nav.Link>

              <Nav.Link
                as={Link as any}
                href={authHydrated && user ? '/profile' : '/login'}
                className={lightText ? 'text-white' : 'text-dark'}
                style={{ display: 'flex', alignItems: 'center' }}
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                  strokeWidth={1.5}
                  stroke="currentColor"
                  style={{ width: 24, height: 24, marginRight: authHydrated && user ? 6 : 0 }}
                >
                  <path strokeLinecap="round" strokeLinejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                </svg>
                {authHydrated && user && (
                  <span style={{ fontSize: '0.85rem', fontWeight: 'bold' }}>
                    Chào {user.fullName.split(' ').pop()?.toUpperCase()}
                  </span>
                )}
              </Nav.Link>
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>

      {isHomePage && (
        <>
          <video
            src="/assets/dia_small.mp4"
            style={{
              position: 'absolute',
              top: 0,
              left: 0,
              width: '100%',
              height: '100%',
              objectFit: 'cover'
            }}
            autoPlay
            loop
            muted
            playsInline
          />
          <div
            style={{
              position: 'absolute',
              inset: 0,
              backgroundColor: 'rgba(33,37,41,0.4)'
            }}
          />
          <div
            style={{
              position: 'absolute',
              inset: 0,
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'center',
              alignItems: 'center',
              textAlign: 'center',
              padding: '0 20px'
            }}
          >
            <motion.h1
              className="text-white"
              variants={heroVariants}
              initial="hidden"
              animate="visible"
              style={{ fontSize: '3.5rem', fontWeight: 'bold', marginBottom: '1.5rem' }}
            >
              Trang sức cao cấp
            </motion.h1>
            <motion.h2
              className="text-white"
              variants={heroVariants}
              initial="hidden"
              animate="visible"
              transition={{ delay: 0.4 }}
              style={{ fontSize: '2rem', marginBottom: '2rem' }}
            >
              Khám phá vẻ đẹp của sự sang trọng
            </motion.h2>
            <Link href="/products">
              <Button
                className="btn-thluxury"
                style={{
                  padding: '10px 30px',
                  borderRadius: 25
                }}
              >
                Khám phá ngay
              </Button>
            </Link>
          </div>
        </>
      )}
    </header>
  );
}
