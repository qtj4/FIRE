import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Grid,
  Paper,
  Stack,
  Typography
} from '@mui/material';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  BarElement,
  ArcElement,
  Tooltip,
  Legend
} from 'chart.js';
import { Bar, Doughnut } from 'react-chartjs-2';
import { PageShell } from '@/components/PageShell';
import { StatCard } from '@/components/StatCard';
import { fetchDashboardStats } from '@/services/stats';
import type { DashboardStats } from '@/types';

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Tooltip, Legend);

const defaultStats: DashboardStats = {
  totals: { tickets: 0, avgPriority: 0, vipShare: 0, inRouting: 0 },
  byCity: [],
  byType: [],
  byOffice: [],
  bySentiment: []
};

export function Dashboard() {
  const [stats, setStats] = useState<DashboardStats>(defaultStats);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    fetchDashboardStats()
      .then((data) => {
        if (!isMounted) return;
        setStats(data);
      })
      .catch((err) => {
        if (!isMounted) return;
        setError(err?.message ?? 'Не удалось загрузить аналитику.');
      })
      .finally(() => {
        if (!isMounted) return;
        setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const byCityData = useMemo(() => {
    return {
      labels: stats.byCity.map((item) => item.city),
      datasets: [
        {
          label: 'Обращения',
          data: stats.byCity.map((item) => item.count),
          backgroundColor: 'rgba(47, 127, 107, 0.6)',
          borderRadius: 10
        }
      ]
    };
  }, [stats.byCity]);

  const byTypeData = useMemo(() => {
    return {
      labels: stats.byType.map((item) => item.type),
      datasets: [
        {
          label: 'Количество',
          data: stats.byType.map((item) => item.count),
          backgroundColor: [
            'rgba(47, 127, 107, 0.75)',
            'rgba(199, 143, 44, 0.7)',
            'rgba(31, 46, 41, 0.6)',
            'rgba(89, 182, 154, 0.7)',
            'rgba(110, 90, 60, 0.6)',
            'rgba(60, 151, 129, 0.7)'
          ],
          borderWidth: 0
        }
      ]
    };
  }, [stats.byType]);

  const bySentimentData = useMemo(() => {
    return {
      labels: stats.bySentiment.map((item) => item.sentiment),
      datasets: [
        {
          data: stats.bySentiment.map((item) => item.count),
          backgroundColor: ['rgba(199, 143, 44, 0.7)', 'rgba(47, 127, 107, 0.65)', 'rgba(31, 46, 41, 0.6)'],
          borderWidth: 0
        }
      ]
    };
  }, [stats.bySentiment]);

  return (
    <PageShell
      title="Дашборд распределения"
      subtitle="Ключевые метрики качества маршрутизации и загрузки менеджеров"
    >
      {loading ? (
        <Typography variant="body1">Загрузка аналитики...</Typography>
      ) : error ? (
        <Alert severity="error">{error}</Alert>
      ) : (
        <Stack spacing={3}>
          <Grid container spacing={2}>
            <Grid item xs={12} md={3}>
              <StatCard
                label="Всего обращений"
                value={stats.totals.tickets.toLocaleString('ru-RU')}
                helper="За текущую смену"
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <StatCard
                label="Средний приоритет"
                value={stats.totals.avgPriority.toFixed(1)}
                helper="По шкале 1-10"
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <StatCard
                label="Доля VIP"
                value={`${Math.round(stats.totals.vipShare * 100)}%`}
                helper="От всех обращений"
              />
            </Grid>
            <Grid item xs={12} md={3}>
              <StatCard
                label="В маршрутизации"
                value={stats.totals.inRouting.toString()}
                helper="Ожидают назначения"
              />
            </Grid>
          </Grid>

          <Grid container spacing={2}>
            <Grid item xs={12} md={7}>
              <Paper
                elevation={0}
                sx={{
                  p: 3,
                  borderRadius: 4,
                  border: '1px solid rgba(10, 21, 18, 0.08)',
                  background: 'rgba(255, 255, 255, 0.85)'
                }}
              >
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
                  География обращений
                </Typography>
                <Bar
                  data={byCityData}
                  options={{
                    responsive: true,
                    plugins: {
                      legend: { display: false }
                    },
                    scales: {
                      x: { grid: { display: false } },
                      y: { grid: { color: 'rgba(10, 21, 18, 0.08)' } }
                    }
                  }}
                />
              </Paper>
            </Grid>
            <Grid item xs={12} md={5}>
              <Paper
                elevation={0}
                sx={{
                  p: 3,
                  borderRadius: 4,
                  border: '1px solid rgba(10, 21, 18, 0.08)',
                  background: 'rgba(255, 255, 255, 0.85)'
                }}
              >
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
                  Тональность обращений
                </Typography>
                <Box sx={{ height: 260 }}>
                  <Doughnut
                    data={bySentimentData}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: {
                        legend: { position: 'bottom' }
                      }
                    }}
                  />
                </Box>
              </Paper>
            </Grid>
          </Grid>

          <Grid container spacing={2}>
            <Grid item xs={12} md={7}>
              <Paper
                elevation={0}
                sx={{
                  p: 3,
                  borderRadius: 4,
                  border: '1px solid rgba(10, 21, 18, 0.08)',
                  background: 'rgba(255, 255, 255, 0.85)'
                }}
              >
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
                  Типы обращений
                </Typography>
                <Bar
                  data={byTypeData}
                  options={{
                    indexAxis: 'y',
                    responsive: true,
                    plugins: {
                      legend: { display: false }
                    },
                    scales: {
                      x: { grid: { color: 'rgba(10, 21, 18, 0.08)' } },
                      y: { grid: { display: false } }
                    }
                  }}
                />
              </Paper>
            </Grid>
            <Grid item xs={12} md={5}>
              <Paper
                elevation={0}
                sx={{
                  p: 3,
                  borderRadius: 4,
                  border: '1px solid rgba(10, 21, 18, 0.08)',
                  background: 'rgba(255, 255, 255, 0.85)'
                }}
              >
                <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
                  Распределение по офисам
                </Typography>
                <Stack spacing={1.5}>
                  {stats.byOffice.map((item) => (
                    <Box key={item.office} sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2">{item.office}</Typography>
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
                        {item.count}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              </Paper>
            </Grid>
          </Grid>
        </Stack>
      )}
    </PageShell>
  );
}
