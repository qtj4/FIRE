import { Paper, Typography } from '@mui/material';
import { PageShell } from '@/components/PageShell';
import { mockManagerProfile } from '@/mocks/manager';

export function ManagerProfile() {
  const m = mockManagerProfile;
  return (
    <PageShell title="Профиль менеджера" subtitle={m.office}>
      <Paper sx={{ p: 4, borderRadius: 4 }}>
        <Typography variant="h6" sx={{ mb: 1 }}>{m.fullName}</Typography>
        <Typography color="text.secondary">{m.role} • {m.department}</Typography>
        <Typography sx={{ mt: 2 }}>Назначено сегодня: {m.stats.assignedToday}</Typography>
        <Typography>В работе: {m.stats.inProgress}</Typography>
      </Paper>
    </PageShell>
  );
}
