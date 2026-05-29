import type { Metadata } from 'next';
import 'bootstrap/dist/css/bootstrap.min.css';
import 'react-multi-carousel/lib/styles.css';
import '@/styles/globals.css';
import Header from '@/components/Header';
import Footer from '@/components/Footer';
import ChatbotLauncher from '@/components/ChatbotLauncher';
import ClientProviders from '@/components/ClientProviders';

export const metadata: Metadata = {
  title: 'THLuxury — Trang sức cao cấp',
  description: 'Khám phá bộ sưu tập trang sức cao cấp tại THLuxury'
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="vi">
      <body>
        <ClientProviders>
          <Header />
          <main>{children}</main>
          <Footer />
          <ChatbotLauncher />
        </ClientProviders>
      </body>
    </html>
  );
}
