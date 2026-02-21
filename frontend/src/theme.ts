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
    button: {
      fontWeight: 700
    },
    h4: {
      fontWeight: 800
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
          fontWeight: 700
        }
      }
    },
    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 999,
          fontWeight: 700
        }
      }
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-root': {
            fontWeight: 700,
            color: 'rgba(10, 21, 18, 0.8)',
            borderBottomColor: 'rgba(10, 21, 18, 0.12)'
          }
        }
      }
    },
    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:last-child td, &:last-child th': {
            borderBottom: 0
          }
        }
      }
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          background:
            'linear-gradient(160deg, rgba(255,255,255,0.96) 0%, rgba(244,251,248,0.94) 70%, rgba(251,246,237,0.94) 100%)'
        }
      }
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          boxShadow: '0 6px 24px rgba(10, 21, 18, 0.05)'
        }
      }
    },
    MuiTextField: {
      styleOverrides: {
        root: {
          '& .MuiOutlinedInput-root': {
            borderRadius: 14
          }
        }
      }
    }
  }
});
