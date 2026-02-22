import { Box, Container, Typography } from '@mui/material';
import type { ReactNode } from 'react';
import type { Breakpoint } from '@mui/material/styles';

interface PageShellProps {
  title: string;
  subtitle?: string;
  actions?: ReactNode;
  children: ReactNode;
  maxWidth?: Breakpoint | false;
}

export function PageShell({ title, subtitle, actions, children, maxWidth = 'lg' }: PageShellProps) {
  return (
    <Container maxWidth={maxWidth} sx={{ py: { xs: 4, md: 6 } }}>
      <Box
        sx={{
          display: 'flex',
          alignItems: { xs: 'flex-start', md: 'center' },
          justifyContent: 'space-between',
          gap: 2.5,
          flexWrap: 'wrap',
          p: { xs: 2.5, md: 3 },
          borderRadius: '12px',
          border: '1px solid rgba(17, 24, 39, 0.08)',
          background: '#ffffff',
          position: 'relative',
          overflow: 'hidden'
        }}
      >
        <Box>
          <Typography variant="overline" sx={{ color: 'text.secondary', letterSpacing: 1 }}>
            Operations Workspace
          </Typography>
          <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.75, letterSpacing: -0.3, lineHeight: 1.15 }}>
            {title}
          </Typography>
          {subtitle ? (
            <Typography variant="body1" sx={{ color: 'text.secondary', maxWidth: 840 }}>
              {subtitle}
            </Typography>
          ) : null}
        </Box>
        {actions ? (
          <Box
            sx={{
              ml: 'auto',
              alignSelf: { xs: 'stretch', md: 'flex-end' },
              pl: { xs: 0, md: 0 }
            }}
          >
            {actions}
          </Box>
        ) : null}
      </Box>
      <Box sx={{ mt: 4 }}>{children}</Box>
    </Container>
  );
}
