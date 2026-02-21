import { AppBar, Box, Button, Toolbar, Typography } from '@mui/material';
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
        background: 'rgba(248, 246, 240, 0.9)',
        backdropFilter: 'blur(12px)'
      }}
    >
      <Toolbar sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: 700, letterSpacing: 0.6 }}>
            FIRE Routing Console
          </Typography>
          <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.6)' }}>
            Intelligent routing for after-hours requests
          </Typography>
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
                  fontWeight: 600,
                  color: isActive ? '#0a1512' : 'rgba(10, 21, 18, 0.75)',
                  borderRadius: 999,
                  px: 2.5,
                  background: isActive ? 'rgba(47, 127, 107, 0.12)' : 'transparent'
                }}
              >
                {item.label}
              </Button>
            );
          })}
        </Box>
      </Toolbar>
    </AppBar>
  );
}
