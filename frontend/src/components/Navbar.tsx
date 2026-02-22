import { AppBar, Box, Button, Chip, Stack, Toolbar, Typography } from '@mui/material';
import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { label: 'Дашборд', to: '/' },
  { label: 'Обращения', to: '/tickets' },
  { label: 'Импорт', to: '/import' }
];

export function Navbar() {
  const location = useLocation();

  return (
    <AppBar
      position="sticky"
      elevation={0}
      sx={{
        borderBottom: '1px solid rgba(17, 24, 39, 0.08)',
        background: 'rgba(255, 255, 255, 0.94)',
        backdropFilter: 'blur(8px)'
      }}
    >
      <Toolbar sx={{ display: 'flex', gap: 2, py: 1, minHeight: 68 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minWidth: 0, flexShrink: 0 }}>
          <Box
            sx={{
              width: 10,
              height: 10,
              borderRadius: '50%',
              background: '#1f6feb'
            }}
          />
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 800, letterSpacing: 0.2, lineHeight: 1.1 }}>
              FIRE Console
            </Typography>
            <Typography variant="caption" sx={{ color: 'text.secondary', display: { xs: 'none', md: 'block' } }}>
              Routing and intake operations
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
                  borderRadius: 999,
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
          <Chip label="API live" size="small" color="success" variant="outlined" />
        </Stack>
      </Toolbar>
    </AppBar>
  );
}
