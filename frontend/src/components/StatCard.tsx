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
        borderRadius: 4,
        border: '1px solid rgba(10, 21, 18, 0.08)',
        background:
          'linear-gradient(145deg, rgba(255, 255, 255, 0.92) 0%, rgba(246, 252, 249, 0.86) 65%, rgba(250, 246, 235, 0.88) 100%)',
        transition: 'transform 200ms ease, box-shadow 200ms ease',
        '&:hover': {
          transform: 'translateY(-2px)',
          boxShadow: '0 14px 30px rgba(10, 21, 18, 0.08)'
        }
      }}
    >
      <Typography variant="overline" sx={{ color: 'rgba(10, 21, 18, 0.62)', letterSpacing: 1.2 }}>
        {label}
      </Typography>
      <Typography variant="h4" sx={{ fontWeight: 800, mb: 0.5, letterSpacing: -0.4 }}>
        {value}
      </Typography>
      {helper ? (
        <Typography variant="body2" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
          {helper}
        </Typography>
      ) : null}
      {icon ? <div>{icon}</div> : null}
    </Paper>
  );
}
