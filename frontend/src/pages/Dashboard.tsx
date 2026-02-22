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
import { ScrollReveal } from '@/components/ScrollReveal';
import { StatCard } from '@/components/StatCard';
import { queryDashboardAssistant } from '@/services/assistant';
import { fetchDashboardStats, fetchInsights, fetchServiceHealth } from '@/services/stats';
import type { AssistantWidgetPlan, DashboardStats, InsightsResponse, ServiceHealth } from '@/types';

ChartJS.register(CategoryScale, LinearScale, BarElement, ArcElement, Tooltip, Legend);

const defaultStats: DashboardStats = {
  totals: { tickets: 0, avgPriority: 0, vipShare: 0, inRouting: 0 },
  byCity: [],
  byType: [],
  byOffice: [],
  bySentiment: [],
  byLanguage: []
};

const panelSx = {
  p: 3,
  borderRadius: '12px',
  border: '1px solid rgba(17, 24, 39, 0.08)',
  background: '#ffffff',
  transition: 'box-shadow 160ms ease',
  '&:hover': {
    boxShadow: '0 10px 24px rgba(17, 24, 39, 0.08)'
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

const accentBarPalette = [
  'rgba(47, 127, 107, 0.75)',
  'rgba(199, 143, 44, 0.7)',
  'rgba(31, 46, 41, 0.6)',
  'rgba(89, 182, 154, 0.7)',
  'rgba(110, 90, 60, 0.6)',
  'rgba(60, 151, 129, 0.7)'
];

function makeMessageId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export function Dashboard() {
  const [stats, setStats] = useState<DashboardStats>(defaultStats);
  const [health, setHealth] = useState<ServiceHealth | null>(null);
  const [insights, setInsights] = useState<InsightsResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [assistantInput, setAssistantInput] = useState('');
  const [assistantBusy, setAssistantBusy] = useState(false);
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

    Promise.all([fetchDashboardStats(), fetchServiceHealth(), fetchInsights()])
      .then(([statsData, healthData, insightsData]) => {
        if (!isMounted) return;
        setStats(statsData);
        setHealth(healthData);
        setInsights(insightsData);
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
          borderRadius: 2
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

  const buildWidgetsFromPlans = (plans: AssistantWidgetPlan[]): AssistantWidget[] => {
    const widgets: AssistantWidget[] = [];
    const topCities = [...stats.byCity].sort((a, b) => b.count - a.count);

    for (const plan of plans.slice(0, 4)) {
      if (plan.kind === 'stat') {
        if (plan.source === 'vipShare') {
          widgets.push({
            id: makeMessageId(),
            kind: 'stat',
            title: plan.title || 'Доля VIP обращений',
            value: `${Math.round(stats.totals.vipShare * 100)}%`,
            helper: plan.helper || 'От всех обращений'
          });
        } else if (plan.source === 'avgPriority') {
          widgets.push({
            id: makeMessageId(),
            kind: 'stat',
            title: plan.title || 'Средний приоритет',
            value: stats.totals.avgPriority.toFixed(1),
            helper: plan.helper || 'По шкале 1-10'
          });
        } else if (plan.source === 'inRouting') {
          widgets.push({
            id: makeMessageId(),
            kind: 'stat',
            title: plan.title || 'В маршрутизации',
            value: stats.totals.inRouting.toLocaleString('ru-RU'),
            helper: plan.helper || 'Ожидают назначения'
          });
        } else if (plan.source === 'ticketsTotal') {
          widgets.push({
            id: makeMessageId(),
            kind: 'stat',
            title: plan.title || 'Всего обращений',
            value: stats.totals.tickets.toLocaleString('ru-RU'),
            helper: plan.helper || 'За текущую смену'
          });
        }
        continue;
      }

      if (plan.kind === 'list' && plan.source === 'topCities') {
        const limit = Math.max(1, Math.min(10, plan.topN ?? 3));
        widgets.push({
          id: makeMessageId(),
          kind: 'list',
          title: plan.title || 'Топ города по обращениям',
          items: topCities.slice(0, limit).map((item) => ({ label: item.city, value: item.count }))
        });
        continue;
      }

      const series =
        plan.source === 'byCity'
          ? stats.byCity.map((item) => ({ label: item.city, value: item.count }))
          : plan.source === 'byType'
            ? stats.byType.map((item) => ({ label: item.type, value: item.count }))
            : plan.source === 'bySentiment'
              ? stats.bySentiment.map((item) => ({ label: item.sentiment, value: item.count }))
              : plan.source === 'byOffice'
                ? stats.byOffice.map((item) => ({ label: item.office, value: item.count }))
                : plan.source === 'byLanguage'
                  ? stats.byLanguage.map((item) => ({ label: item.language, value: item.count }))
                  : [];
      if (!series.length) continue;

      const limit = Math.max(1, Math.min(15, plan.topN ?? series.length));
      const clipped = series.slice(0, limit);
      if (plan.kind === 'list') {
        widgets.push({
          id: makeMessageId(),
          kind: 'list',
          title: plan.title,
          items: clipped.map((item) => ({ label: item.label, value: item.value }))
        });
        continue;
      }

      if (plan.kind === 'bar') {
        widgets.push({
          id: makeMessageId(),
          kind: 'bar',
          title: plan.title,
          data: {
            labels: clipped.map((item) => item.label),
            datasets: [
              {
                label: 'Количество',
                data: clipped.map((item) => item.value),
                backgroundColor:
                  plan.source === 'byCity' ? 'rgba(47, 127, 107, 0.6)' : accentBarPalette,
                borderRadius: 2
              }
            ]
          },
          options: plan.orientation === 'horizontal' ? { indexAxis: 'y' } : undefined
        });
        continue;
      }

      if (plan.kind === 'doughnut') {
        widgets.push({
          id: makeMessageId(),
          kind: 'doughnut',
          title: plan.title,
          data: {
            labels: clipped.map((item) => item.label),
            datasets: [
              {
                data: clipped.map((item) => item.value),
                backgroundColor: clipped.map((_, index) => accentBarPalette[index % accentBarPalette.length]),
                borderWidth: 0
              }
            ]
          }
        });
      }
    }

    return widgets;
  };

  const handleAssistantSubmit = async () => {
    const trimmed = assistantInput.trim();
    if (!trimmed || assistantBusy) return;

    const history = assistantMessages.slice(-8).map((message) => ({
      role: message.role,
      content: message.content
    }));

    setAssistantInput('');
    setAssistantBusy(true);
    setAssistantMessages((prev) => [...prev, { id: makeMessageId(), role: 'user', content: trimmed }]);

    try {
      const result = await queryDashboardAssistant({
        query: trimmed,
        history
      });
      const widgets = buildWidgetsFromPlans(Array.isArray(result.widgets) ? result.widgets : []);
      const content = (result.reply || '').trim() || 'Построил виджеты по вашему запросу.';
      setAssistantMessages((prev) => [
        ...prev,
        {
          id: makeMessageId(),
          role: 'assistant',
          content,
          widgets
        }
      ]);
    } catch (err) {
      const fallbackWidgets = buildWidgetsFromPlans([
        { kind: 'bar', source: 'byType', title: 'Типы обращений', orientation: 'horizontal' },
        { kind: 'bar', source: 'byCity', title: 'География обращений' }
      ]);
      setAssistantMessages((prev) => [
        ...prev,
        {
          id: makeMessageId(),
          role: 'assistant',
          content:
            err instanceof Error
              ? `Не удалось обратиться к AI-сервису (${err.message}). Построил базовые виджеты.`
              : 'Не удалось обратиться к AI-сервису. Построил базовые виджеты.',
          widgets: fallbackWidgets
        }
      ]);
    } finally {
      setAssistantBusy(false);
    }
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
            {[
              { label: 'Всего обращений', value: stats.totals.tickets.toLocaleString('ru-RU'), helper: 'За текущую смену' },
              { label: 'Средний приоритет', value: stats.totals.avgPriority.toFixed(1), helper: 'По шкале 1-10' },
              { label: 'Доля VIP', value: `${Math.round(stats.totals.vipShare * 100)}%`, helper: 'От всех обращений' },
              { label: 'В маршрутизации', value: stats.totals.inRouting.toString(), helper: 'Ожидают назначения' }
            ].map((card, i) => (
              <Grid item xs={12} md={3} key={card.label}>
                <ScrollReveal delay={i * 0.08} enableHover>
                  <StatCard label={card.label} value={card.value} helper={card.helper} />
                </ScrollReveal>
              </Grid>
            ))}
          </Grid>

          <Grid container spacing={2}>
            <Grid item xs={12} md={4}>
              <ScrollReveal delay={0.1} enableHover>
                <Paper elevation={0} sx={panelSx}>
                  <Typography variant="h6" sx={{ mb: 1.5, fontWeight: 700 }}>
                    Статус системы
                  </Typography>
                <Stack spacing={1}>
                  <Chip
                    size="small"
                    label={health?.status === 'UP' ? 'API доступен' : 'Проблемы API'}
                    color={health?.status === 'UP' ? 'success' : 'warning'}
                    sx={{ width: 'fit-content' }}
                  />
                  <Typography variant="body2">Назначено: {health?.assignedTotal ?? 0}</Typography>
                  <Typography variant="body2">В очереди: {health?.unassignedTotal ?? 0}</Typography>
                  <Typography variant="body2">Срочные в очереди: {health?.highPriorityUnassigned ?? 0}</Typography>
                </Stack>
                </Paper>
              </ScrollReveal>
            </Grid>
            <Grid item xs={12} md={4}>
              <ScrollReveal delay={0.15} enableHover>
                <Paper elevation={0} sx={panelSx}>
                  <Typography variant="h6" sx={{ mb: 1.5, fontWeight: 700 }}>
                    Языки обращений
                  </Typography>
                <Stack spacing={1.2}>
                  {stats.byLanguage.map((item) => (
                    <Box key={item.language} sx={{ display: 'flex', justifyContent: 'space-between' }}>
                      <Typography variant="body2">{item.language}</Typography>
                      <Typography variant="body2" sx={{ fontWeight: 700 }}>
                        {item.count}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
                </Paper>
              </ScrollReveal>
            </Grid>
            <Grid item xs={12} md={4}>
              <ScrollReveal delay={0.2} enableHover>
                <Paper elevation={0} sx={panelSx}>
                  <Typography variant="h6" sx={{ mb: 1.5, fontWeight: 700 }}>
                    Рекомендации
                  </Typography>
                <Stack spacing={1.1}>
                  {(insights?.items ?? []).slice(0, 3).map((item) => (
                    <Box key={item.title}>
                      <Typography variant="body2" sx={{ fontWeight: 700 }}>
                        {item.title}
                      </Typography>
                      <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.7)' }}>
                        {item.detail}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
                </Paper>
              </ScrollReveal>
            </Grid>
          </Grid>

          <Grid container spacing={2}>
            <Grid item xs={12} md={7}>
              <ScrollReveal delay={0.1} enableHover>
                <Paper elevation={0} sx={panelSx}>
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
              </ScrollReveal>
            </Grid>
            <Grid item xs={12} md={5}>
              <ScrollReveal delay={0.15} enableHover>
                <Paper elevation={0} sx={panelSx}>
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
              </ScrollReveal>
            </Grid>
          </Grid>

          <Grid container spacing={2}>
            <Grid item xs={12} md={7}>
              <ScrollReveal delay={0.1} enableHover>
                <Paper elevation={0} sx={panelSx}>
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
              </ScrollReveal>
            </Grid>
            <Grid item xs={12} md={5}>
              <ScrollReveal delay={0.15} enableHover>
                <Paper elevation={0} sx={panelSx}>
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
              </ScrollReveal>
            </Grid>
          </Grid>

          <ScrollReveal delay={0.2} enableHover>
            <Paper
              elevation={0}
              sx={{
                ...panelSx,
                background: 'rgba(255, 255, 255, 0.92)'
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
                    onClick={() => !assistantBusy && setAssistantInput(prompt)}
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
                      borderRadius: '6px',
                      background:
                        message.role === 'user'
                          ? 'rgba(47, 127, 107, 0.10)'
                          : 'rgba(10, 21, 18, 0.04)'
                    }}
                  >
                    <Typography variant="body2" sx={{ whiteSpace: 'pre-line' }}>
                      {message.content}
                    </Typography>
                  </Box>
                ))}
                {assistantBusy ? (
                  <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.65)' }}>
                    Ассистент анализирует запрос...
                  </Typography>
                ) : null}
              </Stack>

              {assistantWidgets.length > 0 ? (
                <Grid container spacing={2}>
                  {assistantWidgets.map((widget) => (
                    <Grid item xs={12} md={widget.kind === 'stat' ? 4 : 6} key={widget.id}>
                      <Paper
                        elevation={0}
                        sx={{
                          p: 2.5,
                          borderRadius: '6px',
                          border: '1px solid rgba(10, 21, 18, 0.08)',
                          background: 'rgba(255, 255, 255, 0.92)'
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
                    if (event.key === 'Enter' && !assistantBusy) {
                      event.preventDefault();
                      void handleAssistantSubmit();
                    }
                  }}
                  disabled={assistantBusy}
                  fullWidth
                />
                <Button
                  variant="contained"
                  disabled={!assistantInput.trim() || assistantBusy}
                  onClick={() => void handleAssistantSubmit()}
                >
                  {assistantBusy ? 'Строю...' : 'Построить'}
                </Button>
              </Stack>
            </Stack>
          </Paper>
          </ScrollReveal>
        </Stack>
      )}
    </PageShell>
  );
}
