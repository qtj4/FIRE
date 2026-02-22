import { AppBar, Avatar, Badge, Box, Button, Chip, Stack, Toolbar, Typography } from '@mui/material';
import { Link, useLocation } from 'react-router-dom';
import { mockManagerProfile } from '@/mocks/manager';

const navItems = [
  { label: 'Дашборд', to: '/' },
  { label: 'Обращения', to: '/tickets' },
  { label: 'Операции', to: '/operations' },
  { label: 'Импорт', to: '/import' },
  { label: 'Профиль', to: '/manager' }
];

export function Navbar() {
  const location = useLocation();
  const manager = mockManagerProfile;
  const isOnline = manager.status === 'online';
  const initials = manager.fullName
    .split(' ')
    .map((chunk: string) => chunk[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();

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
      <Toolbar sx={{ display: 'flex', justifyContent: 'space-between', gap: 2.5, py: 0.75 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, minWidth: 0 }}>
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
            <Typography variant="h6" sx={{ fontWeight: 800, letterSpacing: 0.45, lineHeight: 1.1 }}>
              FIRE Routing Console
            </Typography>
            <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.62)' }}>
              Corporate operations workspace
            </Typography>
          </Box>
          <Chip
            label="PROD"
            size="small"
            sx={{
              ml: 1,
              fontWeight: 700,
              letterSpacing: 0.25,
              color: '#1f2e29',
              background: 'rgba(199, 143, 44, 0.22)'
            }}
          />
        </Box>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', justifyContent: 'center' }}>
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
        <Box sx={{ display: { xs: 'none', lg: 'block' }, minWidth: 250 }}>
          <Stack direction="row" spacing={1.25} alignItems="center" justifyContent="flex-end">
            <Badge
              color={isOnline ? 'success' : 'default'}
              overlap="circular"
              variant="dot"
              anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
            >
              <Avatar
                sx={{
                  width: 36,
                  height: 36,
                  fontWeight: 800,
                  fontSize: 13,
                  bgcolor: 'rgba(47, 127, 107, 0.16)',
                  color: '#1f2e29'
                }}
              >
                {initials}
              </Avatar>
            </Badge>
            <Box sx={{ textAlign: 'right' }}>
              <Typography variant="body2" sx={{ fontWeight: 700, lineHeight: 1.1 }}>
                {manager.fullName}
              </Typography>
              <Typography variant="caption" sx={{ color: 'rgba(10, 21, 18, 0.58)' }}>
                {manager.role} • {manager.office}
              </Typography>
            </Box>
            <Chip
              label={isOnline ? 'Online' : 'Offline'}
              size="small"
              color={isOnline ? 'success' : 'default'}
              variant={isOnline ? 'filled' : 'outlined'}
            />
          </Stack>
        </Box>
      </Toolbar>
    </AppBar>
  );
}
