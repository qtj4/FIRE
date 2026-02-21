import { useMemo } from 'react';
import { Avatar, Box, Chip, Divider, Grid, Paper, Stack, Typography } from '@mui/material';
import { PageShell } from '@/components/PageShell';
import { StatCard } from '@/components/StatCard';
import { mockManagerProfile } from '@/mocks/manager';

const panelSx = {
  p: 3,
  borderRadius: 4,
  border: '1px solid rgba(10, 21, 18, 0.08)',
  background:
    'linear-gradient(145deg, rgba(255,255,255,0.94) 0%, rgba(246,252,249,0.88) 64%, rgba(251,246,236,0.9) 100%)'
};

export function ManagerProfile() {
  const manager = mockManagerProfile;

  const initials = useMemo(() => {
    return manager.fullName
      .split(' ')
      .map((chunk: string) => chunk[0])
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }, [manager.fullName]);

  return (
    <PageShell
      title="Профиль менеджера"
      subtitle="Корпоративная карточка сотрудника, загрузка и ключевые операционные показатели"
    >
      <Stack spacing={3}>
        <Grid container spacing={2}>
          <Grid item xs={12} md={3}>
            <StatCard label="Назначено сегодня" value={String(manager.stats.assignedToday)} helper="По текущей смене" />
          </Grid>
          <Grid item xs={12} md={3}>
            <StatCard label="В работе" value={String(manager.stats.inProgress)} helper="Активные обращения" />
          </Grid>
          <Grid item xs={12} md={3}>
            <StatCard label="SLA нарушения" value={String(manager.stats.slaBreaches)} helper="За сегодня" />
          </Grid>
          <Grid item xs={12} md={3}>
            <StatCard label="Средний TTR" value={`${manager.stats.avgHandleTimeMin} мин`} helper="Время обработки" />
          </Grid>
        </Grid>

        <Grid container spacing={2}>
          <Grid item xs={12} md={7}>
            <Paper elevation={0} sx={panelSx}>
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2.5} alignItems={{ xs: 'flex-start', sm: 'center' }}>
                <Avatar
                  sx={{
                    width: 72,
                    height: 72,
                    fontWeight: 800,
                    bgcolor: 'rgba(47, 127, 107, 0.2)',
                    color: '#1f2e29'
                  }}
                >
                  {initials}
                </Avatar>
                <Box>
                  <Typography variant="h5" sx={{ fontWeight: 800, letterSpacing: -0.3 }}>
                    {manager.fullName}
                  </Typography>
                  <Typography variant="body1" sx={{ color: 'rgba(10, 21, 18, 0.72)' }}>
                    {manager.role}
                  </Typography>
                  <Stack direction="row" spacing={1} mt={1.2} flexWrap="wrap">
                    <Chip size="small" label={manager.office} />
                    <Chip size="small" variant="outlined" label={manager.department} />
                    <Chip
                      size="small"
                      label={manager.status === 'online' ? 'Online' : 'Offline'}
                      color={manager.status === 'online' ? 'success' : 'default'}
                    />
                  </Stack>
                </Box>
              </Stack>

              <Divider sx={{ my: 2.5 }} />

              <Grid container spacing={2}>
                <Grid item xs={12} sm={6}>
                  <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.58)' }}>
                    Корпоративная почта
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>
                    {manager.email}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.58)' }}>
                    Рабочий телефон
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>
                    {manager.phone}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.58)' }}>
                    Смена
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>
                    {manager.shift}
                  </Typography>
                </Grid>
                <Grid item xs={12} sm={6}>
                  <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.58)' }}>
                    Идентификатор
                  </Typography>
                  <Typography variant="body2" sx={{ fontWeight: 700 }}>
                    {manager.id}
                  </Typography>
                </Grid>
              </Grid>
            </Paper>
          </Grid>

          <Grid item xs={12} md={5}>
            <Paper elevation={0} sx={panelSx}>
              <Typography variant="h6" sx={{ fontWeight: 700, mb: 1.5 }}>
                Компетенции
              </Typography>
              <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.58)' }}>
                Языки
              </Typography>
              <Stack direction="row" spacing={1} flexWrap="wrap" mt={1} mb={2}>
                {manager.languages.map((language) => (
                  <Chip key={language} label={language} size="small" />
                ))}
              </Stack>

              <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.58)' }}>
                Ключевые навыки
              </Typography>
              <Stack spacing={1} mt={1}>
                {manager.skills.map((skill) => (
                  <Box
                    key={skill}
                    sx={{
                      px: 1.5,
                      py: 1,
                      borderRadius: 2,
                      border: '1px solid rgba(10, 21, 18, 0.08)',
                      bgcolor: 'rgba(255,255,255,0.72)'
                    }}
                  >
                    <Typography variant="body2" sx={{ fontWeight: 600 }}>
                      {skill}
                    </Typography>
                  </Box>
                ))}
              </Stack>
            </Paper>
          </Grid>
        </Grid>
      </Stack>
    </PageShell>
  );
}
