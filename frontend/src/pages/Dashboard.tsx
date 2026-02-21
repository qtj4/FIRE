import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Grid,
  Paper,
  Stack,
  TextField,
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

const panelSx = {
  p: 3,
  borderRadius: 4,
  border: '1px solid rgba(10, 21, 18, 0.08)',
  background:
    'linear-gradient(150deg, rgba(255,255,255,0.93) 0%, rgba(244,251,248,0.86) 68%, rgba(251,246,237,0.88) 100%)',
  transition: 'transform 180ms ease, box-shadow 180ms ease',
  '&:hover': {
    transform: 'translateY(-2px)',
    boxShadow: '0 14px 30px rgba(10, 21, 18, 0.08)'
  }
};

type AssistantWidget =
  | {
      id: string;
      kind: 'bar';
      title: string;
      data: {
        labels: string[];
        datasets: Array<{ label?: string; data: number[]; backgroundColor: string | string[]; borderRadius?: number }>;
      };
      options?: Record<string, unknown>;
    }
  | {
      id: string;
      kind: 'doughnut';
      title: string;
      data: {
        labels: string[];
        datasets: Array<{ data: number[]; backgroundColor: string[]; borderWidth?: number }>;
      };
      options?: Record<string, unknown>;
    }
  | {
      id: string;
      kind: 'list';
      title: string;
      items: Array<{ label: string; value: string | number }>;
    }
  | {
      id: string;
      kind: 'stat';
      title: string;
      value: string;
      helper?: string;
    };

interface AssistantMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  widgets?: AssistantWidget[];
}

const quickPrompts = [
  'Покажи распределение типов обращений по городам',
  'Какая доля VIP обращений?',
  'Покажи тональность обращений',
  'Где больше всего обращений?'
];

export function Dashboard() {
  const [stats, setStats] = useState<DashboardStats>(defaultStats);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [assistantInput, setAssistantInput] = useState('');
  const [assistantMessages, setAssistantMessages] = useState<AssistantMessage[]>([
    {
      id: 'assistant-welcome',
      role: 'assistant',
      content:
        'Я помогу построить виджеты по данным маршрутизации. Сформулируйте запрос, например: “Покажи распределение типов обращений по городам”.'
    }
  ]);

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

  const assistantWidgets = useMemo(
    () => assistantMessages.flatMap((message) => message.widgets ?? []),
    [assistantMessages]
  );

  const handleAssistantSubmit = () => {
    const trimmed = assistantInput.trim();
    if (!trimmed) return;

    const normalize = (value: string) =>
      value
        .toLowerCase()
        .replace(/[.,!?;:()]/g, ' ')
        .replace(/\s+/g, ' ')
        .trim();

    const tokens = normalize(trimmed);
    const wantsCity = /город|географ|населен|регион|област/.test(tokens);
    const wantsType = /тип|категор/.test(tokens);
    const wantsSentiment = /тональн|эмоци|настроен/.test(tokens);
    const wantsOffice = /офис|подраздел|бизнес-единиц/.test(tokens);
    const wantsVip = /vip|премиум/.test(tokens);
    const wantsPriority = /приоритет|срочн/.test(tokens);
    const wantsVolume = /больше|топ|максим|лидер/.test(tokens);

    const widgets: AssistantWidget[] = [];
    const makeId = () => `${Date.now()}-${Math.random().toString(16).slice(2)}`;

    if (wantsType) {
      widgets.push({
        id: makeId(),
        kind: 'bar',
        title: 'Типы обращений',
        data: byTypeData,
        options: { indexAxis: 'y' }
      });
    }
    if (wantsCity || wantsVolume) {
      widgets.push({
        id: makeId(),
        kind: 'bar',
        title: 'География обращений',
        data: byCityData
      });
    }
    if (wantsSentiment) {
      widgets.push({
        id: makeId(),
        kind: 'doughnut',
        title: 'Тональность обращений',
        data: bySentimentData
      });
    }
    if (wantsOffice) {
      widgets.push({
        id: makeId(),
        kind: 'list',
        title: 'Распределение по офисам',
        items: stats.byOffice.map((item) => ({ label: item.office, value: item.count }))
      });
    }
    if (wantsVip) {
      widgets.push({
        id: makeId(),
        kind: 'stat',
        title: 'Доля VIP обращений',
        value: `${Math.round(stats.totals.vipShare * 100)}%`,
        helper: 'От всех обращений'
      });
    }
    if (wantsPriority) {
      widgets.push({
        id: makeId(),
        kind: 'stat',
        title: 'Средний приоритет',
        value: stats.totals.avgPriority.toFixed(1),
        helper: 'По шкале 1-10'
      });
    }
    if (wantsVolume && stats.byCity.length > 0) {
      const topCities = [...stats.byCity].sort((a, b) => b.count - a.count).slice(0, 3);
      widgets.push({
        id: makeId(),
        kind: 'list',
        title: 'Топ города по обращениям',
        items: topCities.map((item) => ({ label: item.city, value: item.count }))
      });
    }

    const response =
      widgets.length > 0
        ? `Готово! Построил виджеты: ${widgets.map((widget) => widget.title).join(', ')}.`
        : 'Не нашел метрик для запроса. Попробуйте: “тип обращений”, “тональность”, “по городам”, “по офисам”, “доля VIP”.';

    setAssistantMessages((prev) => [
      ...prev,
      { id: makeId(), role: 'user', content: trimmed },
      { id: makeId(), role: 'assistant', content: response, widgets }
    ]);
    setAssistantInput('');
  };

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
                sx={panelSx}
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
                sx={panelSx}
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
                sx={panelSx}
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
                sx={panelSx}
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

          <Paper
            elevation={0}
            sx={{
              ...panelSx,
              background:
                'linear-gradient(145deg, rgba(255,255,255,0.95) 0%, rgba(245,251,248,0.9) 70%, rgba(253,247,237,0.9) 100%)'
            }}
          >
            <Stack spacing={2}>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 2 }}>
                <Box>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    AI-ассистент
                  </Typography>
                  <Typography variant="body2" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
                    Задайте вопрос, и ассистент соберет виджеты под запрос.
                  </Typography>
                </Box>
                <Button
                  variant="text"
                  size="small"
                  onClick={() => {
                    const last = [...assistantMessages].reverse().find((message) => message.role === 'assistant');
                    if (last) {
                      navigator.clipboard?.writeText(last.content);
                    }
                  }}
                >
                  Копировать ответ
                </Button>
              </Box>

              <Stack direction="row" spacing={1} flexWrap="wrap">
                {quickPrompts.map((prompt) => (
                  <Chip
                    key={prompt}
                    label={prompt}
                    size="small"
                    onClick={() => setAssistantInput(prompt)}
                    sx={{
                      cursor: 'pointer',
                      border: '1px solid rgba(47, 127, 107, 0.22)',
                      background: 'rgba(47, 127, 107, 0.08)',
                      '&:hover': {
                        background: 'rgba(47, 127, 107, 0.18)'
                      }
                    }}
                  />
                ))}
              </Stack>

              <Stack spacing={1.5}>
                {assistantMessages.map((message) => (
                  <Box
                    key={message.id}
                    sx={{
                      alignSelf: message.role === 'user' ? 'flex-end' : 'flex-start',
                      maxWidth: '88%',
                      p: 2,
                      borderRadius: 3,
                      background:
                        message.role === 'user'
                          ? 'linear-gradient(120deg, rgba(47, 127, 107, 0.22), rgba(89, 182, 154, 0.16))'
                          : 'rgba(10, 21, 18, 0.04)'
                    }}
                  >
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-line' }}>
                      {message.content}
                    </Typography>
                  </Box>
                ))}
              </Stack>

              {assistantWidgets.length > 0 ? (
                <Grid container spacing={2}>
                  {assistantWidgets.map((widget) => (
                    <Grid item xs={12} md={widget.kind === 'stat' ? 4 : 6} key={widget.id}>
                      <Paper
                        elevation={0}
                        sx={{
                          p: 2.5,
                          borderRadius: 3,
                          border: '1px solid rgba(10, 21, 18, 0.08)',
                          background:
                            'linear-gradient(145deg, rgba(255,255,255,0.94) 0%, rgba(246,252,249,0.88) 65%, rgba(251,246,236,0.9) 100%)'
                        }}
                      >
                        <Typography variant="subtitle1" sx={{ fontWeight: 700, mb: 1.5 }}>
                          {widget.title}
                        </Typography>
                        {widget.kind === 'bar' ? (
                          <Bar
                            data={widget.data}
                            options={{
                              responsive: true,
                              plugins: { legend: { display: false } },
                              scales: {
                                x: { grid: { display: false } },
                                y: { grid: { color: 'rgba(10, 21, 18, 0.08)' } }
                              },
                              ...widget.options
                            }}
                          />
                        ) : null}
                        {widget.kind === 'doughnut' ? (
                          <Box sx={{ height: 220 }}>
                            <Doughnut
                              data={widget.data}
                              options={{
                                responsive: true,
                                maintainAspectRatio: false,
                                plugins: { legend: { position: 'bottom' } },
                                ...widget.options
                              }}
                            />
                          </Box>
                        ) : null}
                        {widget.kind === 'list' ? (
                          <Stack spacing={1}>
                            {widget.items.map((item) => (
                              <Box key={item.label} sx={{ display: 'flex', justifyContent: 'space-between' }}>
                                <Typography variant="body2">{item.label}</Typography>
                                <Typography variant="body2" sx={{ fontWeight: 600 }}>
                                  {item.value}
                                </Typography>
                              </Box>
                            ))}
                          </Stack>
                        ) : null}
                        {widget.kind === 'stat' ? (
                          <Box>
                            <Typography variant="h4" sx={{ fontWeight: 700 }}>
                              {widget.value}
                            </Typography>
                            {widget.helper ? (
                              <Typography variant="body2" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
                                {widget.helper}
                              </Typography>
                            ) : null}
                          </Box>
                        ) : null}
                      </Paper>
                    </Grid>
                  ))}
                </Grid>
              ) : null}

              <Stack direction={{ xs: 'column', md: 'row' }} spacing={2}>
                <TextField
                  label="Запрос ассистенту"
                  placeholder="Например, покажи распределение типов обращений по городам"
                  value={assistantInput}
                  onChange={(event) => setAssistantInput(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === 'Enter') {
                      event.preventDefault();
                      handleAssistantSubmit();
                    }
                  }}
                  fullWidth
                />
                <Button variant="contained" disabled={!assistantInput.trim()} onClick={handleAssistantSubmit}>
                  Построить
                </Button>
              </Stack>
            </Stack>
          </Paper>
        </Stack>
      )}
    </PageShell>
  );
}
