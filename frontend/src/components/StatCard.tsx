import { Paper, Typography } from '@mui/material';
import type { ReactNode } from 'react';

interface StatCardProps {
  label: string;
  value: string;
  helper?: string;
  icon?: ReactNode;
}

export function StatCard({ label, value, helper, icon }: StatCardProps) {
  return (
    <Paper
      elevation={0}
      sx={{
        p: 3,
        borderRadius: '12px',
        border: '1px solid rgba(17, 24, 39, 0.08)',
        background: '#ffffff',
        transition: 'box-shadow 160ms ease, transform 160ms ease',
        '&:hover': {
          boxShadow: '0 10px 24px rgba(17, 24, 39, 0.08)',
          transform: 'translateY(-1px)'
        }
      }}
    >
      <Typography variant="overline" sx={{ color: 'text.secondary', letterSpacing: 0.8 }}>
        {label}
      </Typography>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5, letterSpacing: -0.3 }}>
        {value}
      </Typography>
      {helper ? (
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {helper}
        </Typography>
      ) : null}
      {icon ? <div>{icon}</div> : null}
    </Paper>
  );
}
