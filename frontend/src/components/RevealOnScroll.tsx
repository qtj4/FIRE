import { Box } from '@mui/material';
import type { ReactNode } from 'react';
import { useEffect, useRef, useState } from 'react';

interface RevealOnScrollProps {
  children: ReactNode;
  delayMs?: number;
  yOffset?: number;
}

export function RevealOnScroll({ children, delayMs = 0, yOffset = 26 }: RevealOnScrollProps) {
  const elementRef = useRef<HTMLDivElement | null>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const node = elementRef.current;
    if (!node) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (!entry.isIntersecting) return;
        setVisible(true);
        observer.disconnect();
      },
      { threshold: 0.2 }
    );

    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  return (
    <Box
      ref={elementRef}
      sx={{
        opacity: visible ? 1 : 0,
        transform: visible ? 'translateY(0px)' : `translateY(${yOffset}px)`,
        transition: `opacity 620ms cubic-bezier(0.21, 0.7, 0.22, 1) ${delayMs}ms, transform 620ms cubic-bezier(0.21, 0.7, 0.22, 1) ${delayMs}ms`
      }}
    >
      {children}
    </Box>
  );
}
