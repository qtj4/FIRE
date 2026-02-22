import { useEffect, useRef, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  LinearProgress,
  Link,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography
} from '@mui/material';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import { PageShell } from '@/components/PageShell';
import { ScrollReveal } from '@/components/ScrollReveal';
import { fetchIntakeResults, uploadTicketsCsv } from '@/services/intake';
import type { IntakeResponse, TicketProcessingResult } from '@/types';

const POLL_INTERVAL_MS = 2000;
const POLL_TIMEOUT_MS = 60000;

export function ImportCenter() {
  const [file, setFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<IntakeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [polling, setPolling] = useState(false);
  const pollUntilRef = useRef<number>(0);
  const pollingGuidsRef = useRef<string[]>([]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    setFile(f ?? null);
    setResult(null);
    setError(null);
  };

  const handleUpload = async () => {
    if (!file) return;
    setLoading(true);
    setError(null);
    setResult(null);
    setPolling(false);
    try {
      const data = await uploadTicketsCsv(file);
      setResult(data);
      const inQueue = data.results?.filter((r) => r.status === 'IN_QUEUE') ?? [];
      if (inQueue.length > 0) {
        pollingGuidsRef.current = (data.results ?? []).map((r) => String(r.clientGuid));
        pollUntilRef.current = Date.now() + POLL_TIMEOUT_MS;
        setPolling(true);
      }
    } catch (err: unknown) {
      const message = err && typeof err === 'object' && 'message' in err ? String((err as Error).message) : 'Ошибка загрузки';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!polling || pollingGuidsRef.current.length === 0) return;
    if (Date.now() > pollUntilRef.current) {
      setPolling(false);
      return;
    }
    const t = setInterval(async () => {
      if (Date.now() > pollUntilRef.current) {
        setPolling(false);
        return;
      }
      try {
        const next = await fetchIntakeResults(pollingGuidsRef.current);
        if (next.length > 0) {
          setResult((prev) =>
            prev ? { ...prev, results: next } : null
          );
          const stillInQueue = next.some((r) => r.status === 'IN_QUEUE');
          if (!stillInQueue) setPolling(false);
        }
      } catch {
        // ignore poll errors
      }
    }, POLL_INTERVAL_MS);
    return () => clearInterval(t);
  }, [polling]);

  const results = result?.results ?? [];
  const successCount = results.filter((r) => r.status === 'ASSIGNED' || r.status === 'ENRICHED' || r.status?.toLowerCase().includes('assign')).length;
  const inQueueCount = results.filter((r) => r.status === 'IN_QUEUE').length;
  const failCount = results.filter((r) => r.status === 'FAILED').length;
  const intakeOrigin = import.meta.env.VITE_INTAKE_API_BASE_URL || 'http://localhost:8082';
  const kafkaUiUrl = intakeOrigin.replace(/:8082$/, ':8090');

  return (
    <PageShell title="Импорт" subtitle="Загрузка тикетов и справочников (CSV → маршрутизация)">
      <Stack spacing={3}>
        <ScrollReveal enableHover delay={0}>
          <Paper
            elevation={0}
            sx={{
              p: 4,
              borderRadius: 4,
              border: '1px solid rgba(10, 21, 18, 0.08)',
              background:
                'linear-gradient(145deg, rgba(255,255,255,0.94) 0%, rgba(246,252,249,0.88) 65%, rgba(251,246,236,0.9) 100%)'
            }}
          >
            <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>
              Загрузка тикетов (CSV)
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              CSV → ticket-intake-service → N8N (обогащение) → Kafka (очередь incoming_tickets) → evaluation-service (назначение офиса и менеджера) → результат в таблице ниже. При необходимости откройте Kafka UI для просмотра очередей.
            </Typography>
            <Stack direction="row" spacing={2} alignItems="center" flexWrap="wrap">
              <Button
                variant="outlined"
                component="label"
                startIcon={<UploadFileIcon />}
                sx={{
                  borderColor: 'rgba(47, 127, 107, 0.5)',
                  color: '#1f2e29',
                  '&:hover': { borderColor: 'rgba(47, 127, 107, 0.8)', bgcolor: 'rgba(47, 127, 107, 0.06)' }
                }}
              >
                Выбрать CSV
                <input type="file" accept=".csv" hidden onChange={handleFileChange} />
              </Button>
              {file && (
                <Chip
                  label={file.name}
                  onDelete={() => setFile(null)}
                  size="small"
                  sx={{ bgcolor: 'rgba(47, 127, 107, 0.12)', color: '#1f2e29' }}
                />
              )}
              <Button
                variant="contained"
                disabled={!file || loading}
                onClick={handleUpload}
                sx={{
                  bgcolor: '#2f7f6b',
                  '&:hover': { bgcolor: '#215546' }
                }}
              >
                {loading ? 'Отправка…' : 'Загрузить'}
              </Button>
            </Stack>
            {loading && <LinearProgress sx={{ mt: 2, borderRadius: 1 }} />}
          </Paper>
        </ScrollReveal>

        {error && (
          <ScrollReveal delay={0.1}>
            <Alert severity="error" onClose={() => setError(null)}>
              {error}
            </Alert>
          </ScrollReveal>
        )}

        {result && (
          <ScrollReveal delay={0.15}>
            <Paper
              elevation={0}
              sx={{
                p: 3,
                borderRadius: 4,
                border: '1px solid rgba(10, 21, 18, 0.08)',
                background:
                  'linear-gradient(145deg, rgba(255,255,255,0.93) 0%, rgba(244,251,248,0.86) 68%, rgba(251,246,237,0.88) 100%)'
              }}
            >
              <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 2 }}>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  Результаты импорта
                </Typography>
                <Chip
                  label={result.status}
                  size="small"
                  color={result.status === 'SUCCESS' ? 'success' : 'default'}
                  sx={{ fontWeight: 600 }}
                />
                <Typography variant="body2" color="text.secondary">
                  Обработано: {result.processedCount}, ошибок: {result.failedCount}
                </Typography>
                {successCount > 0 && (
                  <Chip label={`Назначено: ${successCount}`} size="small" sx={{ bgcolor: 'rgba(47, 127, 107, 0.2)' }} />
                )}
                {inQueueCount > 0 && (
                  <Chip label={polling ? `В очереди: ${inQueueCount} (обновление…)` : `В очереди: ${inQueueCount}`} size="small" color="info" variant="outlined" />
                )}
                {failCount > 0 && (
                  <Chip label={`Ошибки: ${failCount}`} size="small" color="error" variant="outlined" />
                )}
                <Link href={kafkaUiUrl} target="_blank" rel="noopener noreferrer" sx={{ display: 'flex', alignItems: 'center', gap: 0.5, fontSize: '0.875rem' }}>
                  Kafka UI <OpenInNewIcon sx={{ fontSize: 16 }} />
                </Link>
              </Stack>
              {results.length > 0 ? (
                <Box sx={{ overflowX: 'auto' }}>
                  <Table size="small" stickyHeader>
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 700 }}>Client GUID</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Статус</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Офис</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Менеджер</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Приоритет</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>Сообщение</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {results.map((row: TicketProcessingResult, index: number) => (
                        <TableRow
                          key={`${row.clientGuid}-${index}`}
                          sx={{
                            '&:hover': { bgcolor: 'rgba(47, 127, 107, 0.04)' },
                            borderLeft:
                              row.status === 'FAILED'
                                ? '4px solid rgba(211, 47, 47, 0.6)'
                                : row.status === 'IN_QUEUE'
                                  ? '4px solid rgba(2, 136, 209, 0.5)'
                                  : '4px solid rgba(47, 127, 107, 0.4)'
                          }}
                        >
                          <TableCell>{row.clientGuid}</TableCell>
                          <TableCell>
                            <Chip
                              label={row.status === 'IN_QUEUE' ? 'В очереди' : row.status}
                              size="small"
                              color={row.status === 'FAILED' ? 'error' : row.status === 'IN_QUEUE' ? 'info' : 'success'}
                              variant={row.status === 'FAILED' ? 'filled' : 'outlined'}
                            />
                          </TableCell>
                          <TableCell>{row.assignedOfficeName ?? '—'}</TableCell>
                          <TableCell>{row.assignedManagerName ?? '—'}</TableCell>
                          <TableCell>{row.priority ?? '—'}</TableCell>
                          <TableCell sx={{ maxWidth: 220 }}>{row.message ?? '—'}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </Box>
              ) : (
                <Typography variant="body2" color="text.secondary">
                  Нет построчных результатов.
                </Typography>
              )}
            </Paper>
          </ScrollReveal>
        )}
      </Stack>
    </PageShell>
  );
}
