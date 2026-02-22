import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  Link,
  Paper,
  Stack,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import AutorenewIcon from '@mui/icons-material/Autorenew';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import PlaylistAddCheckIcon from '@mui/icons-material/PlaylistAddCheck';
import { PageShell } from '@/components/PageShell';
import { fetchRecentIntakeResults } from '@/services/intake';
import { assignTicket, createTicket, deleteTicket, fetchTickets, updateTicket } from '@/services/tickets';
import type { Ticket, TicketMutationPayload, TicketProcessingResult } from '@/types';

const panelSx = {
  p: 3,
  borderRadius: 4,
  border: '1px solid rgba(10, 21, 18, 0.08)',
  background:
    'linear-gradient(145deg, rgba(255,255,255,0.94) 0%, rgba(246,252,249,0.88) 64%, rgba(251,246,236,0.9) 100%)'
};

const queuePollMs = 4000;
const defaultForm: TicketMutationPayload = {
  clientGuid: '',
  type: '',
  priority: 5,
  summary: '',
  language: 'RU',
  sentiment: 'Нейтральный'
};

export function OperationsCenter() {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [ticketLoading, setTicketLoading] = useState(true);
  const [ticketError, setTicketError] = useState<string | null>(null);

  const [queueItems, setQueueItems] = useState<TicketProcessingResult[]>([]);
  const [queueLoading, setQueueLoading] = useState(true);
  const [queueError, setQueueError] = useState<string | null>(null);
  const [queueLimit, setQueueLimit] = useState(50);
  const [autoRefreshQueue, setAutoRefreshQueue] = useState(true);

  const [formOpen, setFormOpen] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editingTicket, setEditingTicket] = useState<Ticket | null>(null);
  const [form, setForm] = useState<TicketMutationPayload>(defaultForm);

  const kafkaUiUrl = import.meta.env.VITE_KAFKA_UI_URL || 'http://localhost:8090';

  const loadTickets = useCallback(async () => {
    try {
      setTicketError(null);
      const data = await fetchTickets();
      setTickets(data);
    } catch (error) {
      setTicketError(error instanceof Error ? error.message : 'Не удалось загрузить обращения.');
    } finally {
      setTicketLoading(false);
    }
  }, []);

  const loadQueue = useCallback(
    async (limit: number) => {
      try {
        setQueueError(null);
        const data = await fetchRecentIntakeResults(limit);
        setQueueItems(data);
      } catch (error) {
        setQueueError(error instanceof Error ? error.message : 'Не удалось загрузить очередь.');
      } finally {
        setQueueLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    void loadTickets();
    void loadQueue(queueLimit);
  }, [loadQueue, loadTickets, queueLimit]);

  useEffect(() => {
    if (!autoRefreshQueue) return undefined;
    const timer = window.setInterval(() => {
      void loadQueue(queueLimit);
    }, queuePollMs);
    return () => window.clearInterval(timer);
  }, [autoRefreshQueue, loadQueue, queueLimit]);

  const queueCounters = useMemo(() => {
    const assigned = queueItems.filter((item) => {
      const status = (item.status ?? '').toUpperCase();
      return status.includes('ASSIGN') || status === 'ENRICHED';
    }).length;
    const inQueue = queueItems.filter((item) => (item.status ?? '').toUpperCase() === 'IN_QUEUE').length;
    const failed = queueItems.filter((item) => {
      const status = (item.status ?? '').toUpperCase();
      return status === 'FAILED' || status === 'UNASSIGNED';
    }).length;
    return { assigned, inQueue, failed };
  }, [queueItems]);

  const openCreateDialog = () => {
    setEditingTicket(null);
    setForm(defaultForm);
    setFormOpen(true);
  };

  const openEditDialog = (ticket: Ticket) => {
    setEditingTicket(ticket);
    setForm({
      rawTicketId: ticket.rawTicketId,
      clientGuid: ticket.clientId ?? '',
      type: ticket.type ?? '',
      priority: ticket.priority ?? 5,
      summary: ticket.summary ?? ticket.description ?? '',
      language: ticket.language ?? '',
      sentiment: ticket.sentiment ?? '',
      latitude: ticket.latitude,
      longitude: ticket.longitude
    });
    setFormOpen(true);
  };

  const closeDialog = () => {
    if (saving) return;
    setFormOpen(false);
    setEditingTicket(null);
    setForm(defaultForm);
  };

  const saveTicket = async () => {
    if (!form.type?.trim()) {
      setTicketError('Укажите тип обращения.');
      return;
    }
    if (!form.summary?.trim()) {
      setTicketError('Укажите summary/описание.');
      return;
    }

    const payload: TicketMutationPayload = {
      ...form,
      type: form.type.trim(),
      summary: form.summary.trim(),
      clientGuid: form.clientGuid?.trim() || undefined
    };

    try {
      setSaving(true);
      setTicketError(null);
      if (editingTicket?.backendId) {
        await updateTicket(editingTicket.backendId, payload);
      } else {
        await createTicket(payload);
      }
      await loadTickets();
      closeDialog();
    } catch (error) {
      setTicketError(error instanceof Error ? error.message : 'Не удалось сохранить тикет.');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (ticket: Ticket) => {
    if (!ticket.backendId) {
      setTicketError('Не удалось удалить: у тикета нет backendId.');
      return;
    }
    const confirmed = window.confirm(`Удалить тикет #${ticket.id}?`);
    if (!confirmed) return;

    try {
      setTicketError(null);
      await deleteTicket(ticket.backendId);
      await loadTickets();
    } catch (error) {
      setTicketError(error instanceof Error ? error.message : 'Не удалось удалить тикет.');
    }
  };

  const handleAssign = async (ticket: Ticket) => {
    if (!ticket.backendId) {
      setTicketError('Не удалось назначить: у тикета нет backendId.');
      return;
    }

    try {
      setTicketError(null);
      await assignTicket(ticket.backendId);
      await loadTickets();
      await loadQueue(queueLimit);
    } catch (error) {
      setTicketError(error instanceof Error ? error.message : 'Не удалось назначить тикет.');
    }
  };

  return (
    <PageShell
      title="Операции"
      subtitle="Мониторинг очереди и базовые CRUD-операции по обращениям"
      actions={
        <Button variant="contained" startIcon={<AddIcon />} onClick={openCreateDialog}>
          Новый тикет
        </Button>
      }
    >
      <Stack spacing={3}>
        <Paper elevation={0} sx={panelSx}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ xs: 'stretch', md: 'center' }} mb={2}>
            <Typography variant="h6" sx={{ fontWeight: 700, flex: 1 }}>
              Очередь обработки (final_distribution)
            </Typography>
            <TextField
              size="small"
              type="number"
              label="Лимит"
              value={queueLimit}
              onChange={(event) => setQueueLimit(Math.max(1, Math.min(500, Number(event.target.value) || 1)))}
              sx={{ width: 120 }}
            />
            <FormControlLabel
              control={
                <Switch
                  checked={autoRefreshQueue}
                  onChange={(event) => setAutoRefreshQueue(event.target.checked)}
                />
              }
              label="Автообновление"
            />
            <Button
              variant="outlined"
              startIcon={<AutorenewIcon />}
              onClick={() => void loadQueue(queueLimit)}
            >
              Обновить
            </Button>
            <Link href={kafkaUiUrl} target="_blank" rel="noopener noreferrer" sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}>
              Kafka UI <OpenInNewIcon sx={{ fontSize: 16 }} />
            </Link>
          </Stack>

          <Stack direction="row" spacing={1} sx={{ mb: 2 }} flexWrap="wrap">
            <Chip label={`Всего: ${queueItems.length}`} />
            <Chip label={`Назначено: ${queueCounters.assigned}`} color="success" variant="outlined" />
            <Chip label={`В очереди: ${queueCounters.inQueue}`} color="info" variant="outlined" />
            <Chip label={`Проблемные: ${queueCounters.failed}`} color="error" variant="outlined" />
          </Stack>

          {queueLoading ? (
            <Typography variant="body2">Загрузка очереди...</Typography>
          ) : queueError ? (
            <Alert severity="error">{queueError}</Alert>
          ) : queueItems.length === 0 ? (
            <Alert severity="info">Пока нет данных в локальном хранилище результатов назначения.</Alert>
          ) : (
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Client GUID</TableCell>
                  <TableCell>Статус</TableCell>
                  <TableCell>Офис</TableCell>
                  <TableCell>Менеджер</TableCell>
                  <TableCell>Raw ID</TableCell>
                  <TableCell>Enriched ID</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {queueItems.map((item, idx) => (
                  <TableRow key={`${item.clientGuid}-${idx}`}>
                    <TableCell>{item.clientGuid}</TableCell>
                    <TableCell>{item.status}</TableCell>
                    <TableCell>{item.assignedOfficeName ?? '—'}</TableCell>
                    <TableCell>{item.assignedManagerName ?? '—'}</TableCell>
                    <TableCell>{item.rawTicketId ?? '—'}</TableCell>
                    <TableCell>{item.enrichedTicketId ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </Paper>

        <Paper elevation={0} sx={panelSx}>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ xs: 'stretch', md: 'center' }} mb={2}>
            <Typography variant="h6" sx={{ fontWeight: 700, flex: 1 }}>
              CRUD тикетов
            </Typography>
            <Button variant="outlined" startIcon={<AutorenewIcon />} onClick={() => void loadTickets()}>
              Обновить
            </Button>
          </Stack>

          {ticketLoading ? (
            <Typography variant="body2">Загрузка тикетов...</Typography>
          ) : ticketError ? (
            <Alert severity="error" sx={{ mb: 2 }}>
              {ticketError}
            </Alert>
          ) : null}

          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Client GUID</TableCell>
                <TableCell>Тип</TableCell>
                <TableCell>Приоритет</TableCell>
                <TableCell>Офис</TableCell>
                <TableCell>Менеджер</TableCell>
                <TableCell>Создан</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {tickets.map((ticket) => (
                <TableRow key={ticket.id}>
                  <TableCell>{ticket.id}</TableCell>
                  <TableCell>{ticket.clientId ?? '—'}</TableCell>
                  <TableCell>{ticket.type}</TableCell>
                  <TableCell>{ticket.priority}</TableCell>
                  <TableCell>{ticket.office ?? '—'}</TableCell>
                  <TableCell>{ticket.assignedManager ?? '—'}</TableCell>
                  <TableCell>{ticket.createdAt ? new Date(ticket.createdAt).toLocaleString('ru-RU') : '—'}</TableCell>
                  <TableCell align="right">
                    <Tooltip title="Назначить менеджера">
                      <IconButton size="small" onClick={() => void handleAssign(ticket)}>
                        <PlaylistAddCheckIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Редактировать">
                      <IconButton size="small" onClick={() => openEditDialog(ticket)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Удалить">
                      <IconButton size="small" color="error" onClick={() => void handleDelete(ticket)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
              {!ticketLoading && tickets.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8}>
                    <Alert severity="info">Тикеты отсутствуют.</Alert>
                  </TableCell>
                </TableRow>
              ) : null}
            </TableBody>
          </Table>
        </Paper>
      </Stack>

      <Dialog open={formOpen} onClose={closeDialog} fullWidth maxWidth="sm">
        <DialogTitle>{editingTicket ? `Редактирование тикета #${editingTicket.id}` : 'Создание тикета'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Client GUID (опционально)"
              value={form.clientGuid ?? ''}
              onChange={(event) => setForm((prev) => ({ ...prev, clientGuid: event.target.value }))}
              fullWidth
            />
            <TextField
              label="Raw Ticket ID (опционально)"
              type="number"
              value={form.rawTicketId ?? ''}
              onChange={(event) =>
                setForm((prev) => ({ ...prev, rawTicketId: event.target.value ? Number(event.target.value) : undefined }))
              }
              fullWidth
            />
            <TextField
              label="Тип обращения"
              value={form.type}
              onChange={(event) => setForm((prev) => ({ ...prev, type: event.target.value }))}
              fullWidth
              required
            />
            <TextField
              label="Приоритет (1-10)"
              type="number"
              value={form.priority}
              onChange={(event) => setForm((prev) => ({ ...prev, priority: Number(event.target.value) || 0 }))}
              inputProps={{ min: 1, max: 10 }}
              fullWidth
            />
            <TextField
              label="Язык"
              value={form.language ?? ''}
              onChange={(event) => setForm((prev) => ({ ...prev, language: event.target.value }))}
              fullWidth
            />
            <TextField
              label="Тональность"
              value={form.sentiment ?? ''}
              onChange={(event) => setForm((prev) => ({ ...prev, sentiment: event.target.value }))}
              fullWidth
            />
            <TextField
              label="Summary"
              value={form.summary}
              onChange={(event) => setForm((prev) => ({ ...prev, summary: event.target.value }))}
              multiline
              minRows={3}
              fullWidth
              required
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeDialog} disabled={saving}>
            Отмена
          </Button>
          <Button onClick={() => void saveTicket()} variant="contained" disabled={saving}>
            {saving ? 'Сохранение...' : 'Сохранить'}
          </Button>
        </DialogActions>
      </Dialog>
    </PageShell>
  );
}
