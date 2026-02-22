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
        borderBottom: '1px solid rgba(10, 21, 18, 0.08)',
        background:
          'linear-gradient(120deg, rgba(251, 248, 240, 0.96) 0%, rgba(242, 251, 247, 0.96) 55%, rgba(251, 243, 229, 0.96) 100%)',
        backdropFilter: 'blur(12px)'
      }}
    >
      <Toolbar sx={{ display: 'flex', gap: 2.5, py: 1, minHeight: 76 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minWidth: 0, flexShrink: 0 }}>
          <Box
            sx={{
              width: 14,
              height: 14,
              borderRadius: '50%',
              background: 'linear-gradient(140deg, #2f7f6b 0%, #59b69a 100%)',
              boxShadow: '0 0 0 6px rgba(47, 127, 107, 0.14)'
            }}
          />
          <Box>
            <Typography variant="h6" sx={{ fontWeight: 800, letterSpacing: 0.3, lineHeight: 1.1 }}>
              FIRE Console
            </Typography>
            <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.62)', display: { xs: 'none', md: 'block' } }}>
              Routing and intake operations
            </Typography>
          </Box>
          <Chip
            label="PROD"
            size="small"
            sx={{
              fontWeight: 700,
              letterSpacing: 0.25,
              color: '#1f2e29',
              background: 'rgba(199, 143, 44, 0.22)'
            }}
          />
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
                  fontWeight: 700,
                  whiteSpace: 'nowrap',
                  color: isActive ? '#0a1512' : 'rgba(10, 21, 18, 0.72)',
                  borderRadius: 999,
                  px: 2.2,
                  background: isActive
                    ? 'linear-gradient(120deg, rgba(47, 127, 107, 0.2), rgba(89, 182, 154, 0.14))'
                    : 'transparent',
                  border: isActive ? '1px solid rgba(47, 127, 107, 0.28)' : '1px solid transparent'
                }}
              >
                {item.label}
              </Button>
            );
          })}
        </Stack>

        <Stack direction="row" spacing={1} sx={{ display: { xs: 'none', xl: 'flex' }, flexShrink: 0 }}>
          <Chip label="API live" size="small" color="success" variant="outlined" />
          <Chip label="Queue monitor" size="small" variant="outlined" sx={{ borderColor: 'rgba(47, 127, 107, 0.35)' }} />
        </Stack>
      </Toolbar>
    </AppBar>
  );
}
