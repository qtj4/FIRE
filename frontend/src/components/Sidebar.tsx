import { Box, Chip, Stack, Typography } from '@mui/material';
import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { label: 'Dashboard', to: '/' },
  { label: 'Tickets', to: '/tickets' },
  { label: 'Import Center', to: '/import' }
];

const secondaryItems = ['Queues', 'SLA Policy', 'Knowledge Base', 'Audit Log'];

export function Sidebar() {
  const location = useLocation();

  return (
    <Box
      sx={{
        width: 272,
        px: 2,
        py: 2,
        borderRight: '1px solid rgba(17, 24, 39, 0.08)',
        background: 'linear-gradient(180deg, #ffffff 0%, #f8fbff 100%)',
        display: { xs: 'none', md: 'flex' },
        flexDirection: 'column',
        gap: 3,
        position: 'sticky',
        top: 0,
        height: '100vh'
      }}
    >
      <Box sx={{ p: 1.5, borderRadius: 2, border: '1px solid rgba(17, 24, 39, 0.08)', background: '#fff' }}>
        <Typography sx={{ fontWeight: 800, letterSpacing: 0.3 }}>FIRE CRM</Typography>
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          Operations and intake suite
        </Typography>
      </Box>

      <Stack spacing={1}>
        <Typography variant="overline" sx={{ color: 'text.secondary', letterSpacing: 0.8 }}>
          Navigation
        </Typography>
        {navItems.map((item) => {
          const isActive = item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to);
          return (
            <Box
              key={item.to}
              component={Link}
              to={item.to}
              sx={{
                p: 1.25,
                borderRadius: 1.5,
                border: isActive ? '1px solid rgba(16, 93, 211, 0.25)' : '1px solid transparent',
                background: isActive ? 'rgba(31, 111, 235, 0.1)' : 'transparent',
                color: isActive ? '#0f172a' : 'rgba(17, 24, 39, 0.8)',
                fontWeight: isActive ? 700 : 600
              }}
            >
              {item.label}
            </Box>
          );
        })}
      </Stack>

      <Stack spacing={1}>
        <Typography variant="overline" sx={{ color: 'text.secondary', letterSpacing: 0.8 }}>
          Workspace
        </Typography>
        {secondaryItems.map((item) => (
          <Box
            key={item}
            sx={{
              px: 1.25,
              py: 1,
              borderRadius: 1.5,
              color: 'rgba(17, 24, 39, 0.7)',
              fontWeight: 600
            }}
          >
            {item}
          </Box>
        ))}
      </Stack>

      <Box sx={{ mt: 'auto', p: 1.5, borderRadius: 2, background: '#0f172a', color: '#e2e8f0' }}>
        <Typography sx={{ fontWeight: 700, mb: 1 }}>System Status</Typography>
        <Stack direction="row" spacing={1} flexWrap="wrap" useFlexGap>
          <Chip size="small" label="API Live" color="success" variant="outlined" />
          <Chip size="small" label="Queue Stable" variant="outlined" sx={{ color: '#e2e8f0', borderColor: '#334155' }} />
        </Stack>
      </Box>
    </Box>
  );
}
