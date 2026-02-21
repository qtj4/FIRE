import { AppBar, Box, Button, Chip, Toolbar, Typography } from '@mui/material';
import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { label: 'Дашборд', to: '/' },
  { label: 'Обращения', to: '/tickets' }
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
          'linear-gradient(120deg, rgba(249, 245, 234, 0.95) 0%, rgba(241, 251, 247, 0.95) 52%, rgba(250, 241, 225, 0.95) 100%)',
        backdropFilter: 'blur(12px)'
      }}
    >
      <Toolbar sx={{ display: 'flex', justifyContent: 'space-between', gap: 2.5, py: 0.5 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
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
            <Typography variant="h6" sx={{ fontWeight: 700, letterSpacing: 0.5, lineHeight: 1.1 }}>
              FIRE Routing Console
            </Typography>
            <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.62)' }}>
              Live dispatch view
            </Typography>
          </Box>
          <Chip
            label="PROD"
            size="small"
            sx={{
              ml: 1,
              fontWeight: 700,
              color: '#1f2e29',
              background: 'rgba(199, 143, 44, 0.22)'
            }}
          />
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          {navItems.map((item) => {
            const isActive = location.pathname === item.to;
            return (
              <Button
                key={item.to}
                component={Link}
                to={item.to}
                sx={{
                  textTransform: 'none',
                  fontWeight: 700,
                  color: isActive ? '#0a1512' : 'rgba(10, 21, 18, 0.72)',
                  borderRadius: 999,
                  px: 2.5,
                  background: isActive
                    ? 'linear-gradient(120deg, rgba(47, 127, 107, 0.24), rgba(89, 182, 154, 0.18))'
                    : 'transparent',
                  border: isActive ? '1px solid rgba(47, 127, 107, 0.28)' : '1px solid transparent'
                }}
              >
                {item.label}
              </Button>
            );
          })}
        </Box>
        <Box sx={{ display: { xs: 'none', md: 'block' } }}>
          <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.55)' }}>
            Intelligent routing for after-hours requests
          </Typography>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
