import { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Chip,
  FormControl,
  InputLabel,
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
import { PageShell } from '@/components/PageShell';
import { fetchTickets } from '@/services/tickets';
import type { Ticket } from '@/types';

const rowsPerPage = 8;

const segmentStyles: Record<string, { bg: string; color: string }> = {
  VIP: { bg: 'rgba(199, 143, 44, 0.2)', color: '#8b5c12' },
  Priority: { bg: 'rgba(47, 127, 107, 0.2)', color: '#215546' },
  Mass: { bg: 'rgba(31, 46, 41, 0.08)', color: '#1f2e29' }
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

  return (
    <PageShell
      title="Список обращений"
      subtitle="Фильтруйте обращения по сегменту, типу и тональности перед распределением"
    >
      <Paper
        elevation={0}
        sx={{
          p: 3,
          borderRadius: 4,
          border: '1px solid rgba(10, 21, 18, 0.08)',
          background: 'rgba(255, 255, 255, 0.85)'
        }}
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
            <Table size="small">
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
                  <TableRow key={ticket.id} hover>
                    <TableCell sx={{ fontWeight: 600 }}>{ticket.id}</TableCell>
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
                      <Typography variant="body2" sx={{ fontWeight: 600 }}>
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
    </PageShell>
  );
}
