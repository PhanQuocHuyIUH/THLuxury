'use client';
import { useState } from 'react';
import { motion } from 'framer-motion';
import ChatbotModal from './ChatbotModal';

export default function ChatbotLauncher() {
  const [show, setShow] = useState(false);

  return (
    <>
      <motion.div
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.9 }}
        onClick={() => setShow(true)}
        style={{
          position: 'fixed',
          bottom: 20,
          right: 20,
          backgroundColor: '#f2e3bf',
          borderRadius: '50%',
          width: 60,
          height: 60,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 4px 15px rgba(0,0,0,0.2)',
          cursor: 'pointer',
          zIndex: 999
        }}
        title="Trợ lý ảo"
      >
        <svg
          xmlns="http://www.w3.org/2000/svg"
          fill="none"
          viewBox="0 0 24 24"
          strokeWidth={1.5}
          stroke="#003468"
          style={{ width: 30, height: 30 }}
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M12 20.25c4.97 0 9-3.694 9-8.25s-4.03-8.25-9-8.25S3 7.444 3 12c0 2.104.859 4.023 2.273 5.48.432.447.74 1.04.586 1.641a4.483 4.483 0 01-.923 1.785A5.969 5.969 0 006 21c1.282 0 2.47-.402 3.445-1.087.81.22 1.668.337 2.555.337z"
          />
        </svg>
      </motion.div>
      <ChatbotModal show={show} onClose={() => setShow(false)} />
    </>
  );
}
