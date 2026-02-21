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
        background: 'rgba(255, 255, 255, 0.85)'
      }}
    >
      <Typography variant="overline" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
        {label}
      </Typography>
      <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5 }}>
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
