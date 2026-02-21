import { ChangeEvent, MouseEvent, useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  Grid,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  ToggleButton,
  ToggleButtonGroup,
  Typography
} from '@mui/material';
import CloudUploadRoundedIcon from '@mui/icons-material/CloudUploadRounded';
import DescriptionRoundedIcon from '@mui/icons-material/DescriptionRounded';
import DownloadRoundedIcon from '@mui/icons-material/DownloadRounded';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import { PageShell } from '@/components/PageShell';
import axios from 'axios';
import { getAllIntakeConfigs, getIntakeConfig, uploadCsv } from '@/services/intake';
import type { CsvUploadRecord, IntakeDataset } from '@/types';

const panelSx = {
  p: 3,
  borderRadius: 4,
  border: '1px solid rgba(10, 21, 18, 0.08)',
  background:
    'linear-gradient(145deg, rgba(255,255,255,0.94) 0%, rgba(246,252,249,0.88) 64%, rgba(251,246,236,0.9) 100%)'
};

const DATASETS: IntakeDataset[] = ['offices', 'managers', 'tickets'];
const historyStorageKey = 'fire-intake-history-v1';

function parseCsvLine(line: string): string[] {
  const result: string[] = [];
  let current = '';
  let inQuotes = false;

  for (let i = 0; i < line.length; i += 1) {
    const char = line[i];
    const nextChar = line[i + 1];

    if (char === '"') {
      if (inQuotes && nextChar === '"') {
        current += '"';
        i += 1;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }

    if (char === ',' && !inQuotes) {
      result.push(current.trim());
      current = '';
      continue;
    }

    current += char;
  }

  result.push(current.trim());
  return result;
}

function readCsvPreview(content: string, maxRows = 6): string[][] {
  const lines = content
    .replace(/^\uFEFF/, '')
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .slice(0, maxRows);

  return lines.map(parseCsvLine);
}

function formatDatasetLabel(dataset: IntakeDataset): string {
  const cfg = getIntakeConfig(dataset);
  return cfg.title;
}

function formatDateTime(value: string): string {
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleString('ru-RU', { dateStyle: 'medium', timeStyle: 'short' });
}

function formatDuration(ms: number): string {
  if (ms < 1000) return `${ms} ms`;
  return `${(ms / 1000).toFixed(2)} s`;
}

function extractApiErrorMessage(payload: unknown): string | null {
  if (!payload || typeof payload !== 'object') return null;
  if ('message' in payload && typeof payload.message === 'string') return payload.message;
  if ('error' in payload && typeof payload.error === 'string') return payload.error;
  return null;
}

export function ImportCenter() {
  const [dataset, setDataset] = useState<IntakeDataset>('tickets');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewRows, setPreviewRows] = useState<string[][]>([]);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [history, setHistory] = useState<CsvUploadRecord[]>([]);

  useEffect(() => {
    try {
      const raw = localStorage.getItem(historyStorageKey);
      if (!raw) return;
      const parsed = JSON.parse(raw) as CsvUploadRecord[];
      if (Array.isArray(parsed)) {
        setHistory(parsed.slice(0, 20));
      }
    } catch {
      // ignore invalid local storage payload
    }
  }, []);

  useEffect(() => {
    localStorage.setItem(historyStorageKey, JSON.stringify(history.slice(0, 20)));
  }, [history]);

  const config = useMemo(() => getIntakeConfig(dataset), [dataset]);
  const allConfigs = useMemo(() => getAllIntakeConfigs(), []);

  const handleDatasetChange = (_event: MouseEvent<HTMLElement>, value: IntakeDataset | null) => {
    if (!value) return;
    setDataset(value);
    setSelectedFile(null);
    setPreviewRows([]);
    setError(null);
  };

  const handleFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0] ?? null;
    setSelectedFile(file);
    setError(null);

    if (!file) {
      setPreviewRows([]);
      return;
    }

    try {
      const text = await file.text();
      setPreviewRows(readCsvPreview(text));
    } catch {
      setPreviewRows([]);
      setError('Не удалось прочитать CSV-файл для предпросмотра.');
    }
  };

  const downloadTemplate = () => {
    const lines = [config.columns.join(','), config.sampleRow.join(',')];
    const blob = new Blob([`\uFEFF${lines.join('\n')}`], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `template-${dataset}.csv`;
    anchor.click();
    URL.revokeObjectURL(url);
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Выберите CSV файл перед отправкой.');
      return;
    }

    setUploading(true);
    setError(null);
    const startedAt = performance.now();

    try {
      const response = await uploadCsv(dataset, selectedFile);
      const record: CsvUploadRecord = {
        ...response,
        id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
        dataset,
        endpoint: config.endpoint,
        fileName: selectedFile.name,
        uploadedAt: new Date().toISOString(),
        durationMs: Math.round(performance.now() - startedAt)
      };

      setHistory((prev) => [record, ...prev].slice(0, 20));
      setSelectedFile(null);
      setPreviewRows([]);
    } catch (uploadError: unknown) {
      const axiosError = axios.isAxiosError(uploadError) ? uploadError : null;
      const responseMessage = extractApiErrorMessage(axiosError?.response?.data);
      const apiMessage =
        responseMessage ??
        (uploadError instanceof Error ? uploadError.message : null) ??
        'Не удалось отправить CSV на сервер.';
      setError(String(apiMessage));
    } finally {
      setUploading(false);
    }
  };

  const latest = history[0];

  return (
    <PageShell
      title="Data Import Center"
      subtitle="Загрузка CSV в intake API с предпросмотром структуры и контролем качества обработки"
    >
      <Stack spacing={2.5} mb={3}>
        <Paper elevation={0} sx={panelSx}>
          <Typography variant="h6" sx={{ fontWeight: 800, mb: 1.2 }}>
            Доступные intake endpoint'ы
          </Typography>
          <Stack direction={{ xs: 'column', md: 'row' }} spacing={1.2} useFlexGap flexWrap="wrap">
            {DATASETS.map((item) => {
              const itemConfig = allConfigs[item];
              return (
                <Chip
                  key={item}
                  icon={<DescriptionRoundedIcon />}
                  label={`${itemConfig.title}: ${itemConfig.endpoint}`}
                  sx={{ justifyContent: 'flex-start', px: 0.7 }}
                />
              );
            })}
          </Stack>
        </Paper>
      </Stack>

      <Grid container spacing={2}>
        <Grid item xs={12} md={7}>
          <Paper elevation={0} sx={panelSx}>
            <Typography variant="h6" sx={{ fontWeight: 800, mb: 1.5 }}>
              Импорт CSV
            </Typography>

            <ToggleButtonGroup
              value={dataset}
              exclusive
              onChange={handleDatasetChange}
              color="primary"
              sx={{ mb: 2, flexWrap: 'wrap' }}
            >
              {DATASETS.map((item) => (
                <ToggleButton key={item} value={item} sx={{ textTransform: 'none', fontWeight: 700 }}>
                  {formatDatasetLabel(item)}
                </ToggleButton>
              ))}
            </ToggleButtonGroup>

            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.2} mb={2}>
              <Button variant="outlined" startIcon={<DownloadRoundedIcon />} onClick={downloadTemplate}>
                Скачать шаблон
              </Button>
              <Button component="label" variant="contained" startIcon={<CloudUploadRoundedIcon />}>
                Выбрать файл
                <input type="file" hidden accept=".csv,text/csv" onChange={handleFileChange} />
              </Button>
              <Button variant="contained" color="secondary" disabled={!selectedFile || uploading} onClick={handleUpload}>
                {uploading ? 'Загрузка...' : 'Отправить в API'}
              </Button>
            </Stack>

            {selectedFile ? (
              <Typography variant="body2" sx={{ fontWeight: 700, mb: 1.6 }}>
                Файл: {selectedFile.name} ({Math.max(1, Math.round(selectedFile.size / 1024))} KB)
              </Typography>
            ) : null}

            <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.62)' }}>
              Обязательные колонки для {config.title}: {config.columns.join(', ')}
            </Typography>

            {error ? <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert> : null}

            {previewRows.length > 0 ? (
              <Box sx={{ mt: 2.2 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 800, mb: 1 }}>
                  Предпросмотр CSV (до 6 строк)
                </Typography>
                <Table size="small">
                  <TableBody>
                    {previewRows.map((row, index) => (
                      <TableRow key={`${row.join('|')}-${index}`}>
                        <TableCell sx={{ width: 36, color: 'rgba(10, 21, 18, 0.55)' }}>{index + 1}</TableCell>
                        <TableCell sx={{ py: 1.1 }}>
                          <Stack direction="row" spacing={0.8} useFlexGap flexWrap="wrap">
                            {row.map((value, colIndex) => (
                              <Chip
                                key={`${value}-${colIndex}`}
                                size="small"
                                label={value || '∅'}
                                variant={index === 0 ? 'filled' : 'outlined'}
                                color={index === 0 ? 'secondary' : 'default'}
                              />
                            ))}
                          </Stack>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Box>
            ) : null}
          </Paper>
        </Grid>

        <Grid item xs={12} md={5}>
          <Paper elevation={0} sx={panelSx}>
            <Stack direction="row" alignItems="center" spacing={1} mb={1.5}>
              <CheckCircleOutlineRoundedIcon color="success" fontSize="small" />
              <Typography variant="h6" sx={{ fontWeight: 800 }}>
                Последний результат
              </Typography>
            </Stack>

            {latest ? (
              <Stack spacing={1.1}>
                <Typography variant="body2">
                  <b>{formatDatasetLabel(latest.dataset)}</b> • {latest.fileName}
                </Typography>
                <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.62)' }}>
                  {formatDateTime(latest.uploadedAt)} • {latest.endpoint}
                </Typography>
                <Divider sx={{ my: 0.4 }} />
                <Stack direction="row" spacing={1}>
                  <Chip label={`Статус: ${latest.status}`} color="success" />
                  <Chip label={`Обработано: ${latest.processedCount}`} variant="outlined" />
                  <Chip label={`Ошибки: ${latest.failedCount}`} variant="outlined" color={latest.failedCount > 0 ? 'error' : 'default'} />
                </Stack>
                <Typography variant="body2" sx={{ color: 'rgba(10, 21, 18, 0.72)' }}>
                  {latest.message}
                </Typography>
                <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.62)' }}>
                  Время обработки запроса: {formatDuration(latest.durationMs)}
                </Typography>
                {latest.dataset === 'tickets' && latest.results && latest.results.length > 0 ? (
                  <Box sx={{ mt: 1.2 }}>
                    <Typography variant="subtitle2" sx={{ fontWeight: 800, mb: 0.8 }}>
                      Assignment results
                    </Typography>
                    <Table size="small">
                      <TableHead>
                        <TableRow>
                          <TableCell>Client GUID</TableCell>
                          <TableCell>Статус</TableCell>
                          <TableCell>Офис</TableCell>
                          <TableCell>Менеджер</TableCell>
                        </TableRow>
                      </TableHead>
                      <TableBody>
                        {latest.results.slice(0, 8).map((result, index) => (
                          <TableRow key={`${result.clientGuid}-${result.enrichedTicketId ?? 'na'}-${index}`}>
                            <TableCell sx={{ maxWidth: 180, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                              {result.clientGuid}
                            </TableCell>
                            <TableCell>
                              <Chip
                                size="small"
                                label={result.status ?? 'UNKNOWN'}
                                color={result.status === 'ASSIGNED' ? 'success' : result.status === 'FAILED' ? 'error' : 'default'}
                                variant={result.status === 'ASSIGNED' ? 'filled' : 'outlined'}
                              />
                            </TableCell>
                            <TableCell>{result.assignedOfficeName ?? '—'}</TableCell>
                            <TableCell>{result.assignedManagerName ?? '—'}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </Box>
                ) : null}
              </Stack>
            ) : (
              <Alert severity="info">История импортов пока пустая.</Alert>
            )}
          </Paper>
        </Grid>
      </Grid>

      <Paper elevation={0} sx={{ ...panelSx, mt: 2.5 }}>
        <Typography variant="h6" sx={{ fontWeight: 800, mb: 1.4 }}>
          История импортов
        </Typography>
        {history.length === 0 ? (
          <Typography variant="body2" sx={{ color: 'rgba(10, 21, 18, 0.65)' }}>
            После первой отправки CSV здесь появятся результаты.
          </Typography>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Дата</TableCell>
                <TableCell>Набор</TableCell>
                <TableCell>Файл</TableCell>
                <TableCell>Processed</TableCell>
                <TableCell>Failed</TableCell>
                <TableCell>Latency</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {history.map((item) => (
                <TableRow key={item.id}>
                  <TableCell>{formatDateTime(item.uploadedAt)}</TableCell>
                  <TableCell>{formatDatasetLabel(item.dataset)}</TableCell>
                  <TableCell>{item.fileName}</TableCell>
                  <TableCell>{item.processedCount}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={item.failedCount}
                      color={item.failedCount > 0 ? 'error' : 'default'}
                      variant={item.failedCount > 0 ? 'filled' : 'outlined'}
                    />
                  </TableCell>
                  <TableCell>{formatDuration(item.durationMs)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>
    </PageShell>
  );
}
