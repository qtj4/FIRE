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
          flexWrap: 'wrap'
        }}
      >
        <Box>
          <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5 }}>
            {title}
          </Typography>
          {subtitle ? (
            <Typography variant="body1" sx={{ color: 'rgba(10, 21, 18, 0.7)' }}>
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
