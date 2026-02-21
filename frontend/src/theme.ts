import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#2f7f6b',
      dark: '#215546',
      light: '#59b69a'
    },
    secondary: {
      main: '#c78f2c'
    },
    background: {
      default: '#f7f3ea',
      paper: 'rgba(255, 255, 255, 0.9)'
    },
    text: {
      primary: '#0a1512',
      secondary: 'rgba(10, 21, 18, 0.7)'
    }
  },
  typography: {
    fontFamily: '"Space Grotesk", "Segoe UI", sans-serif',
    h4: {
      fontWeight: 700
    },
    h6: {
      fontWeight: 700
    }
  },
  shape: {
    borderRadius: 16
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 999,
          fontWeight: 600
        }
      }
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none'
        }
      }
    }
  }
});
