import { Box, Container, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface PageShellProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  children: ReactNode;
}

export function PageShell({ title, subtitle, actions, children }: PageShellProps) {
  return (
    <Container maxWidth="lg" sx={{ py: { xs: 4, md: 6 } }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: { xs: 'flex-start', md: 'center' },
          justifyContent: 'space-between',
          gap: 3,
          flexWrap: 'wrap',
          p: { xs: 2.5, md: 3 },
          borderRadius: 4,
          border: '1px solid rgba(10, 21, 18, 0.08)',
          background:
            'linear-gradient(120deg, rgba(255, 255, 255, 0.88) 0%, rgba(241, 251, 247, 0.82) 55%, rgba(255, 247, 235, 0.86) 100%)'
        }}
      >
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 800, mb: 0.5, letterSpacing: -0.5 }}>
            {title}
          </Typography>
          {subtitle ? (
            <Typography variant="body1" sx={{ color: 'rgba(10, 21, 18, 0.72)', maxWidth: 760 }}>
              {subtitle}
            </Typography>
          ) : null}
        </Box>
        {actions ? <Box>{actions}</Box> : null}
      </Box>
      <Box sx={{ mt: 4 }}>{children}</Box>
    </Container>
  );
}
