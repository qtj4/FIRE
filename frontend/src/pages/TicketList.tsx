import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Chip,
  Divider,
  Drawer,
  FormControl,
  InputLabel,
  IconButton,
  MenuItem,
  Pagination,
  Paper,
  Select,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Typography
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import { PageShell } from '@/components/PageShell';
import { fetchTickets } from '@/services/tickets';
import type { Ticket } from '@/types';

const rowsPerPage = 8;

const segmentStyles: Record<string, { bg: string; color: string }> = {
  VIP: { bg: 'rgba(199, 143, 44, 0.2)', color: '#8b5c12' },
  Priority: { bg: 'rgba(47, 127, 107, 0.2)', color: '#215546' },
  Mass: { bg: 'rgba(31, 46, 41, 0.08)', color: '#1f2e29' }
};

const panelSx = {
  p: 3,
  borderRadius: 4,
  border: '1px solid rgba(10, 21, 18, 0.08)',
  background:
    'linear-gradient(145deg, rgba(255,255,255,0.94) 0%, rgba(246,252,249,0.88) 64%, rgba(251,246,236,0.9) 100%)'
};

export function TicketList() {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [segment, setSegment] = useState('all');
  const [type, setType] = useState('all');
  const [sentiment, setSentiment] = useState('all');
  const [query, setQuery] = useState('');
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null);

  useEffect(() => {
    let isMounted = true;

    fetchTickets()
      .then((data) => {
        if (!isMounted) return;
        setTickets(data);
      })
      .catch((err) => {
        if (!isMounted) return;
        setError(err?.message ?? 'Не удалось загрузить список обращений.');
      })
      .finally(() => {
        if (!isMounted) return;
        setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, []);

  const segments = useMemo(
    () => Array.from(new Set(tickets.map((item) => item.segment))).filter(Boolean),
    [tickets]
  );
  const types = useMemo(
    () => Array.from(new Set(tickets.map((item) => item.type))).filter(Boolean),
    [tickets]
  );
  const sentiments = useMemo(
    () => Array.from(new Set(tickets.map((item) => item.sentiment))).filter(Boolean),
    [tickets]
  );

  const filtered = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return tickets.filter((ticket) => {
      if (segment !== 'all' && ticket.segment !== segment) return false;
      if (type !== 'all' && ticket.type !== type) return false;
      if (sentiment !== 'all' && ticket.sentiment !== sentiment) return false;

      if (!normalizedQuery) return true;
      const haystack = [ticket.description, ticket.summary, ticket.city, ticket.office]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      return haystack.includes(normalizedQuery);
    });
  }, [tickets, segment, type, sentiment, query]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / rowsPerPage));
  const paginated = filtered.slice((page - 1) * rowsPerPage, page * rowsPerPage);

  useEffect(() => {
    setPage(1);
  }, [segment, type, sentiment, query]);

  const formatDate = (value?: string, withTime = false) => {
    if (!value) return '—';
    const parsed = new Date(value);
    if (Number.isNaN(parsed.getTime())) return value;
    return parsed.toLocaleString(
      'ru-RU',
      withTime ? { dateStyle: 'medium', timeStyle: 'short' } : { dateStyle: 'medium' }
    );
  };

  const formatValue = (value?: string | number) =>
    value === null || value === undefined || value === '' ? '—' : String(value);

  const coords =
    selectedTicket && selectedTicket.latitude !== undefined && selectedTicket.longitude !== undefined
      ? `${selectedTicket.latitude.toFixed(5)}, ${selectedTicket.longitude.toFixed(5)}`
      : '—';

  return (
    <PageShell
      title="Список обращений"
      subtitle="Фильтруйте обращения по сегменту, типу и тональности перед распределением"
    >
      <Paper
        elevation={0}
        sx={panelSx}
      >
        <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} mb={3}>
          <TextField
            label="Поиск"
            placeholder="Описание, город, офис"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            fullWidth
          />
          <FormControl sx={{ minWidth: 170 }}>
            <InputLabel id="segment-label">Сегмент</InputLabel>
            <Select
              labelId="segment-label"
              label="Сегмент"
              value={segment}
              onChange={(event) => setSegment(event.target.value)}
            >
              <MenuItem value="all">Все</MenuItem>
              {segments.map((item) => (
                <MenuItem key={item} value={item}>
                  {item}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl sx={{ minWidth: 190 }}>
            <InputLabel id="type-label">Тип обращения</InputLabel>
            <Select
              labelId="type-label"
              label="Тип обращения"
              value={type}
              onChange={(event) => setType(event.target.value)}
            >
              <MenuItem value="all">Все</MenuItem>
              {types.map((item) => (
                <MenuItem key={item} value={item}>
                  {item}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl sx={{ minWidth: 170 }}>
            <InputLabel id="sentiment-label">Тональность</InputLabel>
            <Select
              labelId="sentiment-label"
              label="Тональность"
              value={sentiment}
              onChange={(event) => setSentiment(event.target.value)}
            >
              <MenuItem value="all">Все</MenuItem>
              {sentiments.map((item) => (
                <MenuItem key={item} value={item}>
                  {item}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Stack>

        {loading ? (
          <Typography variant="body1">Загрузка обращений...</Typography>
        ) : error ? (
          <Alert severity="error">{error}</Alert>
        ) : (
          <>
            <Table
              size="small"
              sx={{
                '& .MuiTableRow-root:hover': {
                  background: 'rgba(47, 127, 107, 0.06)'
                }
              }}
            >
              <TableHead>
                <TableRow>
                  <TableCell>ID</TableCell>
                  <TableCell>Сегмент</TableCell>
                  <TableCell>Тип</TableCell>
                  <TableCell>Описание</TableCell>
                  <TableCell>Город</TableCell>
                  <TableCell>Приоритет</TableCell>
                  <TableCell>Менеджер</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {paginated.map((ticket) => (
                  <TableRow
                    key={ticket.id}
                    hover
                    sx={{ cursor: 'pointer' }}
                    onClick={() => setSelectedTicket(ticket)}
                  >
                    <TableCell sx={{ fontWeight: 700 }}>{ticket.id}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={ticket.segment}
                        sx={{
                          backgroundColor: segmentStyles[ticket.segment]?.bg ?? 'rgba(31, 46, 41, 0.08)',
                          color: segmentStyles[ticket.segment]?.color ?? '#1f2e29',
                          fontWeight: 600
                        }}
                      />
                    </TableCell>
                    <TableCell>{ticket.type}</TableCell>
                    <TableCell sx={{ maxWidth: 320 }}>
                      <Typography variant="body2" sx={{ fontWeight: 700 }}>
                        {ticket.description}
                      </Typography>
                      {ticket.summary ? (
                        <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
                          {ticket.summary}
                        </Typography>
                      ) : null}
                    </TableCell>
                    <TableCell>{ticket.city ?? '—'}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        variant="outlined"
                        label={`${ticket.priority}/10`}
                        sx={{ fontWeight: 600 }}
                      />
                    </TableCell>
                    <TableCell>{ticket.assignedManager ?? '—'}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>

            {filtered.length === 0 ? (
              <Box sx={{ mt: 3 }}>
                <Alert severity="info">Нет обращений по выбранным фильтрам.</Alert>
              </Box>
            ) : (
              <Stack direction="row" justifyContent="space-between" alignItems="center" mt={3}>
                <Typography variant="body2" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
                  Найдено: {filtered.length}
                </Typography>
                <Pagination
                  count={totalPages}
                  page={page}
                  onChange={(_, value) => setPage(value)}
                  color="standard"
                />
              </Stack>
            )}
          </>
        )}
      </Paper>

      <Drawer
        anchor="right"
        open={Boolean(selectedTicket)}
        onClose={() => setSelectedTicket(null)}
        PaperProps={{
          sx: {
            width: { xs: '100%', sm: 420, md: 480 },
            p: 3,
            borderLeft: '1px solid rgba(10, 21, 18, 0.08)'
          }
        }}
      >
        {selectedTicket ? (
          <Stack spacing={2}>
            <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', gap: 2 }}>
              <Box>
                <Typography variant="overline" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
                  Обращение
                </Typography>
                <Typography variant="h6" sx={{ fontWeight: 700 }}>
                  {selectedTicket.id}
                </Typography>
                <Stack direction="row" spacing={1} mt={1} alignItems="center" flexWrap="wrap">
                  <Chip size="small" label={selectedTicket.segment} />
                  <Chip size="small" variant="outlined" label={`${selectedTicket.priority}/10`} />
                  {selectedTicket.sentiment ? <Chip size="small" label={selectedTicket.sentiment} /> : null}
                  {selectedTicket.language ? <Chip size="small" label={selectedTicket.language} /> : null}
                </Stack>
              </Box>
              <IconButton aria-label="close" onClick={() => setSelectedTicket(null)}>
                <CloseIcon />
              </IconButton>
            </Box>

            <Divider />

            <Stack spacing={1}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                Клиент
              </Typography>
              <Typography variant="body2">GUID клиента: {formatValue(selectedTicket.clientId)}</Typography>
              <Typography variant="body2">Пол: {formatValue(selectedTicket.gender)}</Typography>
              <Typography variant="body2">Дата рождения: {formatDate(selectedTicket.birthDate)}</Typography>
              <Typography variant="body2">Сегмент: {formatValue(selectedTicket.segment)}</Typography>
            </Stack>

            <Divider />

            <Stack spacing={1}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                Обращение и AI-анализ
              </Typography>
              <Typography variant="body2">Тип: {formatValue(selectedTicket.type)}</Typography>
              <Typography variant="body2">Тональность: {formatValue(selectedTicket.sentiment)}</Typography>
              <Typography variant="body2">Приоритет: {formatValue(selectedTicket.priority)}</Typography>
              <Typography variant="body2">Язык: {formatValue(selectedTicket.language)}</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600 }}>
                Описание
              </Typography>
              <Typography variant="body2">{formatValue(selectedTicket.description)}</Typography>
              <Typography variant="body2" sx={{ fontWeight: 600, mt: 1 }}>
                Summary и рекомендация
              </Typography>
              <Typography variant="body2">{formatValue(selectedTicket.summary)}</Typography>
            </Stack>

            <Divider />

            <Stack spacing={1}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                Адрес и гео-нормализация
              </Typography>
              <Typography variant="body2">Страна: {formatValue(selectedTicket.country)}</Typography>
              <Typography variant="body2">Область: {formatValue(selectedTicket.region)}</Typography>
              <Typography variant="body2">Населенный пункт: {formatValue(selectedTicket.city)}</Typography>
              <Typography variant="body2">Улица: {formatValue(selectedTicket.street)}</Typography>
              <Typography variant="body2">Дом: {formatValue(selectedTicket.house)}</Typography>
              <Typography variant="body2">Координаты: {coords}</Typography>
            </Stack>

            <Divider />

            <Stack spacing={1}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                Назначение
              </Typography>
              <Typography variant="body2">Офис: {formatValue(selectedTicket.office)}</Typography>
              <Typography variant="body2">Менеджер: {formatValue(selectedTicket.assignedManager)}</Typography>
              <Typography variant="body2">
                Дата обращения: {formatDate(selectedTicket.createdAt, true)}
              </Typography>
            </Stack>

            <Divider />

            <Stack spacing={1}>
              <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                Вложения
              </Typography>
              {selectedTicket.attachments && selectedTicket.attachments.length > 0 ? (
                <Stack direction="row" spacing={1} flexWrap="wrap">
                  {selectedTicket.attachments.map((item) => (
                    <Chip key={item} size="small" variant="outlined" label={item} />
                  ))}
                </Stack>
              ) : (
                <Typography variant="body2">—</Typography>
              )}
            </Stack>
          </Stack>
        ) : null}
      </Drawer>
    </PageShell>
  );
}
