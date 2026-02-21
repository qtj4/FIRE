import { Box, Paper, Typography } from '@mui/material';
import { PageShell } from '@/components/PageShell';

export function ImportCenter() {
  return (
    <PageShell title="Импорт" subtitle="Загрузка тикетов и справочников">
      <Paper sx={{ p: 4, borderRadius: 4 }}>
        <Typography color="text.secondary">
          Центр импорта: загрузка CSV с тикетами, офисами и менеджерами.
        </Typography>
        <Box sx={{ mt: 2 }} />
      </Paper>
    </PageShell>
  );
}
