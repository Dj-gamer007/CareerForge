import { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from '@/lib/queryClient';
import { useAuthStore } from '@/features/auth/authStore';
import { AppRouter } from '@/router/AppRouter';

export function App() {
  const initSession = useAuthStore((state) => state.initSession);

  useEffect(() => {
    initSession();
  }, [initSession]);

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppRouter />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
