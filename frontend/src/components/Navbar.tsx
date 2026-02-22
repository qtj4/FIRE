import { AppBar, Box, Button, Chip, Stack, Toolbar, Typography } from '@mui/material';
import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { label: 'Dashboard', to: '/' },
  { label: 'Tickets', to: '/tickets' },
  { label: 'Import', to: '/import' }
];

export function Navbar() {
  const location = useLocation();

  return (
    <AppBar
      position="sticky"
      elevation={0}
      sx={{
        borderBottom: '1px solid rgba(17, 24, 39, 0.08)',
        background: 'rgba(255, 255, 255, 0.9)',
        backdropFilter: 'blur(10px)'
      }}
    >
      <Toolbar sx={{ display: 'flex', gap: 2, py: 1, minHeight: 72 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minWidth: 0, flexShrink: 0 }}>
          <Box
            sx={{
              width: 30,
              height: 30,
              borderRadius: 1.5,
              background: 'linear-gradient(140deg, #1f6feb 0%, #0f3c82 100%)',
              display: 'grid',
              placeItems: 'center',
              color: '#fff',
              fontSize: 12,
              fontWeight: 800
            }}
          >
            FI
          </Box>
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 800, letterSpacing: 0.2, lineHeight: 1 }}>
              FIRE Console
            </Typography>
            <Typography variant="caption" sx={{ color: 'text.secondary', display: { xs: 'none', lg: 'block' } }}>
              Operational command panel
            </Typography>
          </Box>
        </Box>

        <Stack
          direction="row"
          spacing={1}
          sx={{
            flex: 1,
            justifyContent: { xs: 'flex-start', md: 'center' },
            overflowX: 'auto',
            '&::-webkit-scrollbar': { display: 'none' }
          }}
        >
          {navItems.map((item) => {
            const isActive = item.to === '/' ? location.pathname === '/' : location.pathname.startsWith(item.to);
            return (
              <Button
                key={item.to}
                component={Link}
                to={item.to}
                sx={{
                  textTransform: 'none',
                  fontWeight: 600,
                  whiteSpace: 'nowrap',
                  color: isActive ? '#0f172a' : 'rgba(17, 24, 39, 0.72)',
                  borderRadius: 1.5,
                  px: 2,
                  background: isActive ? 'rgba(31, 111, 235, 0.1)' : 'transparent',
                  border: isActive ? '1px solid rgba(31, 111, 235, 0.22)' : '1px solid transparent'
                }}
              >
                {item.label}
              </Button>
            );
          })}
        </Stack>

        <Stack direction="row" spacing={1} sx={{ display: { xs: 'none', xl: 'flex' }, flexShrink: 0 }}>
          <Chip label="SLA 98.7%" size="small" variant="outlined" />
          <Chip label="API live" size="small" color="success" variant="outlined" />
          <Chip label="Queue normal" size="small" color="primary" variant="outlined" />
        </Stack>
      </Toolbar>
    </AppBar>
  );
}
