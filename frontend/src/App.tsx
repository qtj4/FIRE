import { Box } from '@mui/material';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Navbar } from '@/components/Navbar';
import { Sidebar } from '@/components/Sidebar';
import { Dashboard } from '@/pages/Dashboard';
import { ImportCenter } from '@/pages/ImportCenter';
import { TicketList } from '@/pages/TicketList';

export function App() {
  return (
    <Box sx={{ minHeight: '100vh', background: 'linear-gradient(180deg, #f4f7fb 0%, #eef3fb 100%)' }}>
      <Box sx={{ display: 'flex', minHeight: '100vh' }}>
        <Sidebar />
        <Box sx={{ flex: 1, minWidth: 0 }}>
          <Navbar />
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/tickets" element={<TicketList />} />
            <Route path="/import" element={<ImportCenter />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Box>
      </Box>
    </Box>
  );
}
