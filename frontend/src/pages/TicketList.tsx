import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  Drawer,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Pagination,
  Paper,
  Select,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
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
type QueueTab = 'all' | 'spam' | 'vip';

const segmentStyles: Record<string, { bg: string; color: string }> = {
  Spam: { bg: 'rgba(220, 38, 38, 0.14)', color: '#991b1b' },
  VIP: { bg: 'rgba(199, 143, 44, 0.2)', color: '#8b5c12' },
  Priority: { bg: 'rgba(47, 127, 107, 0.2)', color: '#215546' },
  Mass: { bg: 'rgba(31, 46, 41, 0.08)', color: '#1f2e29' }
};

const panelSx = {
  p: 3,
  borderRadius: '12px',
  border: '1px solid rgba(17, 24, 39, 0.08)',
  background: '#ffffff'
};

const isSpamTicket = (ticket: Ticket) => {
  const typeValue = (ticket.type ?? '').toLowerCase();
  const summaryValue = (ticket.summary ?? '').toLowerCase();
  const descriptionValue = (ticket.description ?? '').toLowerCase();
  return (
    ticket.segment === 'Spam' ||
    typeValue.includes('spam') ||
    typeValue.includes('спам') ||
    summaryValue.includes('spam') ||
    summaryValue.includes('спам') ||
    descriptionValue.includes('spam') ||
    descriptionValue.includes('спам')
  );
};

const isVipTicket = (ticket: Ticket) => {
  const segmentValue = (ticket.segment ?? '').toLowerCase();
  const typeValue = (ticket.type ?? '').toLowerCase();
  return segmentValue === 'vip' || typeValue.includes('vip') || ticket.priority >= 8;
};

export function TicketList() {
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(1);
  const [segment, setSegment] = useState('all');
  const [type, setType] = useState('all');
  const [sentiment, setSentiment] = useState('all');
  const [queueTab, setQueueTab] = useState<QueueTab>('all');
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
  const tabCounts = useMemo(
    () => ({
      all: tickets.length,
      spam: tickets.filter(isSpamTicket).length,
      vip: tickets.filter((ticket) => !isSpamTicket(ticket) && isVipTicket(ticket)).length
    }),
    [tickets]
  );

  const filtered = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    return tickets.filter((ticket) => {
      if (queueTab === 'spam' && !isSpamTicket(ticket)) return false;
      if (queueTab === 'vip' && (isSpamTicket(ticket) || !isVipTicket(ticket))) return false;
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
  }, [tickets, queueTab, segment, type, sentiment, query]);

  const totalPages = Math.max(1, Math.ceil(filtered.length / rowsPerPage));
  const paginated = filtered.slice((page - 1) * rowsPerPage, page * rowsPerPage);
  const avgPriority = filtered.length ? filtered.reduce((sum, item) => sum + item.priority, 0) / filtered.length : 0;
  const unassignedCount = filtered.filter((item) => !item.assignedManager).length;
  const highPriorityCount = filtered.filter((item) => item.priority >= 8).length;
  const activeFiltersCount =
    [segment, type, sentiment].filter((value) => value !== 'all').length +
    (query.trim() ? 1 : 0) +
    (queueTab === 'all' ? 0 : 1);
  const updatedAtLabel = new Date().toLocaleString('ru-RU', { dateStyle: 'short', timeStyle: 'short' });

  useEffect(() => {
    setPage(1);
  }, [queueTab, segment, type, sentiment, query]);

  useEffect(() => {
    if (page > totalPages) {
      setPage(totalPages);
    }
  }, [page, totalPages]);

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

  const resetFilters = () => {
    setQueueTab('all');
    setQuery('');
    setSegment('all');
    setType('all');
    setSentiment('all');
  };

  const coords =
    selectedTicket && selectedTicket.latitude !== undefined && selectedTicket.longitude !== undefined
      ? `${selectedTicket.latitude.toFixed(5)}, ${selectedTicket.longitude.toFixed(5)}`
      : '—';

  return (
    <PageShell
      title="Список обращений"
      subtitle="Операционный реестр обращений: фильтрация, приоритеты и доступ к деталям в одном рабочем окне"
      maxWidth="xl"
      actions={
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} flexWrap="wrap">
          <Chip size="small" label={`Записи: ${filtered.length}`} />
          <Chip size="small" variant="outlined" label={`Фильтры: ${activeFiltersCount}`} />
          <Chip size="small" variant="outlined" label={`Обновлено: ${updatedAtLabel}`} />
        </Stack>
      }
    >
      <Stack spacing={2.5}>
        <Paper elevation={0} sx={panelSx}>
          <Stack spacing={2}>
            <Tabs
              value={queueTab}
              onChange={(_, value: QueueTab) => setQueueTab(value)}
              variant="scrollable"
              allowScrollButtonsMobile
              sx={{ borderBottom: '1px solid rgba(17, 24, 39, 0.08)' }}
            >
              <Tab value="all" label={`All (${tabCounts.all})`} />
              <Tab value="spam" label={`Spam (${tabCounts.spam})`} />
              <Tab value="vip" label={`VIP (${tabCounts.vip})`} />
            </Tabs>
            <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.5} alignItems={{ xs: 'stretch', md: 'center' }}>
              <TextField
                label="Поиск"
                placeholder="Описание, summary, город или офис"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                fullWidth
              />
              <FormControl sx={{ minWidth: { xs: '100%', md: 170 } }}>
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
              <FormControl sx={{ minWidth: { xs: '100%', md: 190 } }}>
                <InputLabel id="type-label">Тип обращения</InputLabel>
                <Select labelId="type-label" label="Тип обращения" value={type} onChange={(event) => setType(event.target.value)}>
                  <MenuItem value="all">Все</MenuItem>
                  {types.map((item) => (
                    <MenuItem key={item} value={item}>
                      {item}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl sx={{ minWidth: { xs: '100%', md: 170 } }}>
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
              <Button variant="outlined" onClick={resetFilters} sx={{ minWidth: 132 }}>
                Сбросить
              </Button>
            </Stack>

            <Grid container spacing={1.5}>
              <Grid item xs={12} sm={6} md={3}>
                <Box sx={{ p: 1.5, border: '1px solid rgba(17, 24, 39, 0.1)', borderRadius: '10px', background: '#fff' }}>
                  <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.6)', textTransform: 'uppercase' }}>
                    Найдено
                  </Typography>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    {filtered.length}
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Box sx={{ p: 1.5, border: '1px solid rgba(17, 24, 39, 0.1)', borderRadius: '10px', background: '#fff' }}>
                  <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.6)', textTransform: 'uppercase' }}>
                    Средний приоритет
                  </Typography>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    {avgPriority.toFixed(1)}
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Box sx={{ p: 1.5, border: '1px solid rgba(17, 24, 39, 0.1)', borderRadius: '10px', background: '#fff' }}>
                  <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.6)', textTransform: 'uppercase' }}>
                    Без назначения
                  </Typography>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    {unassignedCount}
                  </Typography>
                </Box>
              </Grid>
              <Grid item xs={12} sm={6} md={3}>
                <Box sx={{ p: 1.5, border: '1px solid rgba(17, 24, 39, 0.1)', borderRadius: '10px', background: '#fff' }}>
                  <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.6)', textTransform: 'uppercase' }}>
                    Высокий приоритет
                  </Typography>
                  <Typography variant="h6" sx={{ fontWeight: 700 }}>
                    {highPriorityCount}
                  </Typography>
                </Box>
              </Grid>
            </Grid>
          </Stack>
        </Paper>

        {loading ? (
          <Paper elevation={0} sx={{ ...panelSx, py: 4 }}>
            <Typography variant="body1">Загрузка обращений...</Typography>
          </Paper>
        ) : error ? (
          <Paper elevation={0} sx={panelSx}>
            <Alert severity="error">{error}</Alert>
          </Paper>
        ) : (
          <Paper elevation={0} sx={panelSx}>
            {filtered.length === 0 ? (
              <Alert severity="info">Нет обращений по выбранным фильтрам.</Alert>
            ) : (
              <>
                <TableContainer sx={{ border: '1px solid rgba(17, 24, 39, 0.08)', borderRadius: '10px', maxHeight: 640 }}>
                  <Table
                    stickyHeader
                    size="small"
                    sx={{
                      tableLayout: 'fixed',
                      width: '100%',
                      minWidth: 1260,
                      '& .MuiTableRow-root:hover': {
                        background: 'rgba(10, 21, 18, 0.03)'
                      }
                    }}
                  >
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ width: '7%', whiteSpace: 'nowrap' }}>ID</TableCell>
                        <TableCell sx={{ width: '8%', whiteSpace: 'nowrap' }}>Сегмент</TableCell>
                        <TableCell sx={{ width: '10%', whiteSpace: 'nowrap' }}>Тип</TableCell>
                        <TableCell sx={{ width: '27%' }}>Описание</TableCell>
                        <TableCell sx={{ width: '7%', whiteSpace: 'nowrap' }}>Language</TableCell>
                        <TableCell sx={{ width: '9%', whiteSpace: 'nowrap' }}>Sentiment</TableCell>
                        <TableCell sx={{ width: '7%', whiteSpace: 'nowrap' }}>Локация</TableCell>
                        <TableCell sx={{ width: '7%', whiteSpace: 'nowrap' }}>Приоритет</TableCell>
                        <TableCell sx={{ width: '11%', whiteSpace: 'nowrap' }}>Менеджер</TableCell>
                        <TableCell sx={{ width: '9%', whiteSpace: 'nowrap' }}>Создано</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {paginated.map((ticket) => (
                        <TableRow key={ticket.id} hover sx={{ cursor: 'pointer' }} onClick={() => setSelectedTicket(ticket)}>
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
                          <TableCell>
                            <Typography variant="body2" sx={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                              {ticket.type}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Typography variant="body2" sx={{ fontWeight: 700, mb: 0.25, lineHeight: 1.35, wordBreak: 'break-word' }}>
                              {ticket.description}
                            </Typography>
                            {ticket.summary ? (
                              <Typography
                                variant="caption"
                                sx={{
                                  color: 'rgba(10, 21, 18, 0.62)',
                                  display: '-webkit-box',
                                  WebkitLineClamp: 3,
                                  WebkitBoxOrient: 'vertical',
                                  overflow: 'hidden'
                                }}
                              >
                                {ticket.summary}
                              </Typography>
                            ) : null}
                          </TableCell>
                          <TableCell>{ticket.language ?? '—'}</TableCell>
                          <TableCell>{ticket.sentiment ?? '—'}</TableCell>
                          <TableCell>{ticket.city ?? '—'}</TableCell>
                          <TableCell>
                            <Chip size="small" variant="outlined" label={`${ticket.priority}/10`} sx={{ fontWeight: 600 }} />
                          </TableCell>
                          <TableCell>
                            <Typography
                              variant="body2"
                              title={ticket.assignedManager ?? '—'}
                              sx={{ whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}
                            >
                              {ticket.assignedManager ?? '—'}
                            </Typography>
                          </TableCell>
                          <TableCell sx={{ whiteSpace: 'nowrap' }}>{formatDate(ticket.createdAt)}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>

                <Stack
                  direction={{ xs: 'column', sm: 'row' }}
                  justifyContent="space-between"
                  alignItems={{ xs: 'flex-start', sm: 'center' }}
                  mt={2.5}
                  spacing={1}
                >
                  <Typography variant="body2" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
                    Записей: {filtered.length} • Страница {page} из {totalPages}
                  </Typography>
                  <Pagination count={totalPages} page={page} onChange={(_, value) => setPage(value)} color="standard" />
                </Stack>
              </>
            )}
          </Paper>
        )}
      </Stack>

      <Drawer
        anchor="right"
        open={Boolean(selectedTicket)}
        onClose={() => setSelectedTicket(null)}
        PaperProps={{
          sx: {
            width: { xs: '100%', sm: 440, md: 520 },
            p: 3,
            borderLeft: '1px solid rgba(10, 21, 18, 0.12)',
            background: 'rgba(255, 255, 255, 0.98)'
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
              <Typography variant="body2">Дата обращения: {formatDate(selectedTicket.createdAt, true)}</Typography>
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
