import { motion } from 'framer-motion';
import type { ReactNode } from 'react';

interface ScrollRevealProps {
  children: ReactNode;
  delay?: number;
  enableHover?: boolean;
}

export function ScrollReveal({ children, delay = 0, enableHover }: ScrollRevealProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 24 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: '-40px' }}
      transition={{
        duration: 0.45,
        delay,
        ease: [0.22, 0.61, 0.36, 1]
      }}
      whileHover={
        enableHover
          ? {
              transition: { duration: 0.2 }
            }
          : undefined
      }
      style={{ willChange: 'opacity, transform' }}
    >
      {children}
    </motion.div>
  );
}
