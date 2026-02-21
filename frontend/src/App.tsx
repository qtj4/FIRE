import { Box } from '@mui/material';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Navbar } from '@/components/Navbar';
import { Dashboard } from '@/pages/Dashboard';
import { TicketList } from '@/pages/TicketList';

export function App() {
  return (
    <Box sx={{ minHeight: '100vh' }}>
      <Navbar />
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/tickets" element={<TicketList />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Box>
  );
}
