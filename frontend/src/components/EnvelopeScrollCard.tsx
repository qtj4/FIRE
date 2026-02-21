import { Box, Chip, Paper, Stack, Typography } from '@mui/material';
import { useEffect, useRef, useState } from 'react';

export function EnvelopeScrollCard() {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const [opened, setOpened] = useState(false);

  useEffect(() => {
    const node = rootRef.current;
    if (!node) return;

    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (!entry.isIntersecting) return;
        setOpened(true);
        observer.disconnect();
      },
      { threshold: 0.35 }
    );

    observer.observe(node);
    return () => observer.disconnect();
  }, []);

  return (
    <Paper
      ref={rootRef}
      elevation={0}
      sx={{
        p: 3,
        borderRadius: 4,
        border: '1px solid rgba(10, 21, 18, 0.08)',
        background:
          'linear-gradient(145deg, rgba(255,255,255,0.95) 0%, rgba(244,251,248,0.9) 62%, rgba(253,247,237,0.92) 100%)',
        overflow: 'hidden',
        position: 'relative'
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          top: -90,
          right: -60,
          width: 230,
          height: 230,
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(47, 127, 107, 0.16) 0%, rgba(47, 127, 107, 0) 70%)'
        }}
      />

      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2.5} alignItems="center">
        <Box sx={{ width: { xs: '100%', md: '52%' } }}>
          <Typography
            variant="h6"
            sx={{
              fontFamily: '"Sora", "Space Grotesk", sans-serif',
              fontWeight: 800,
              mb: 1
            }}
          >
            Routing Mailroom
          </Typography>
          <Typography variant="body2" sx={{ color: 'rgba(10, 21, 18, 0.72)', mb: 1.5 }}>
            При скролле конверт открывается и показывает карточку распределения. Это можно использовать как бренд-анимацию
            в hero-блоке или на onboarding-экране.
          </Typography>
          <Stack direction="row" spacing={1} useFlexGap flexWrap="wrap">
            <Chip size="small" label="Scroll-triggered" color="success" />
            <Chip size="small" label="Corporate motion" variant="outlined" />
          </Stack>
        </Box>

        <Box
          sx={{
            width: { xs: '100%', md: '48%' },
            display: 'flex',
            justifyContent: 'center',
            pb: 1
          }}
        >
          <Box sx={{ position: 'relative', width: 260, height: 190, perspective: '1100px' }}>
            <Box
              sx={{
                position: 'absolute',
                left: 20,
                right: 20,
                bottom: opened ? 78 : 36,
                height: 108,
                borderRadius: 2,
                border: '1px solid rgba(10, 21, 18, 0.08)',
                background:
                  'linear-gradient(145deg, rgba(255,255,255,0.98) 0%, rgba(244,251,248,0.93) 76%, rgba(252,242,225,0.94) 100%)',
                boxShadow: '0 10px 24px rgba(10, 21, 18, 0.12)',
                transition:
                  'bottom 760ms cubic-bezier(0.2, 0.75, 0.18, 1), transform 760ms cubic-bezier(0.2, 0.75, 0.18, 1)',
                transform: opened ? 'translateY(0) rotate(0deg)' : 'translateY(0) rotate(-2deg)',
                px: 1.6,
                py: 1.2,
                overflow: 'hidden',
                zIndex: 2
              }}
            >
              <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
                Assignment Memo
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 800, lineHeight: 1.1 }}>
                VIP ticket -> Аида Нурланова
              </Typography>
              <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
                Office: Алматы Центр
              </Typography>
            </Box>

            <Box
              sx={{
                position: 'absolute',
                left: 0,
                right: 0,
                bottom: 0,
                height: 120,
                borderRadius: 2.5,
                border: '1px solid rgba(10, 21, 18, 0.12)',
                background:
                  'linear-gradient(150deg, rgba(242, 223, 188, 0.95) 0%, rgba(232, 204, 160, 0.95) 60%, rgba(224, 188, 135, 0.92) 100%)',
                boxShadow: '0 18px 34px rgba(10, 21, 18, 0.18)',
                zIndex: 3,
                overflow: 'hidden'
              }}
            >
              <Box
                sx={{
                  position: 'absolute',
                  inset: 0,
                  clipPath: 'polygon(0 100%, 50% 40%, 100% 100%, 100% 100%, 0 100%)',
                  background: 'rgba(255, 255, 255, 0.28)'
                }}
              />
            </Box>

            <Box
              sx={{
                position: 'absolute',
                left: 0,
                right: 0,
                bottom: 70,
                height: 72,
                transformOrigin: '50% 100%',
                transform: opened ? 'rotateX(-178deg)' : 'rotateX(0deg)',
                transition: 'transform 880ms cubic-bezier(0.22, 0.7, 0.2, 1)',
                clipPath: 'polygon(0 100%, 50% 0, 100% 100%)',
                background:
                  'linear-gradient(145deg, rgba(235, 206, 160, 0.98) 0%, rgba(226, 190, 141, 0.97) 65%, rgba(218, 172, 108, 0.96) 100%)',
                borderTop: '1px solid rgba(10, 21, 18, 0.12)',
                zIndex: 4,
                boxShadow: '0 6px 12px rgba(10, 21, 18, 0.16)'
              }}
            />
          </Box>
        </Box>
      </Stack>
    </Paper>
  );
}
