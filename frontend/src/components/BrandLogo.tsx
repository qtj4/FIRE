import { Box, Typography } from '@mui/material';

interface BrandLogoProps {
  size?: number;
}

export function BrandLogo({ size = 40 }: BrandLogoProps) {
  return (
    <Box
      sx={{
        width: size,
        height: size,
        borderRadius: 2.5,
        position: 'relative',
        overflow: 'hidden',
        flexShrink: 0,
        border: '1px solid rgba(10, 21, 18, 0.12)',
        background:
          'linear-gradient(145deg, rgba(255,255,255,0.98) 0%, rgba(241,251,247,0.93) 55%, rgba(252,242,225,0.95) 100%)',
        boxShadow: '0 10px 20px rgba(10, 21, 18, 0.08)'
      }}
    >
      <Box
        sx={{
          position: 'absolute',
          inset: 0,
          background:
            'linear-gradient(130deg, rgba(47, 127, 107, 0.78) 0%, rgba(89, 182, 154, 0.64) 52%, rgba(199, 143, 44, 0.5) 100%)',
          clipPath: 'polygon(0 0, 72% 0, 34% 100%, 0 100%)'
        }}
      />
      <Typography
        sx={{
          position: 'absolute',
          right: 5,
          top: 3,
          fontFamily: '"Sora", "Space Grotesk", sans-serif',
          fontWeight: 900,
          fontSize: size > 36 ? 21 : 18,
          lineHeight: 1,
          color: '#0a1512',
          textShadow: '0 2px 6px rgba(255, 255, 255, 0.55)'
        }}
      >
        F
      </Typography>
    </Box>
  );
}
