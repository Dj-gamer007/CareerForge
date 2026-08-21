import { useEffect } from 'react';
import { BrowserRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { queryClient } from '@/lib/queryClient';
import { useAuthStore } from '@/features/auth/authStore';
import { AppRouter } from '@/router/AppRouter';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export function App() {
  const initSession = useAuthStore((state) => state.initSession);
  const isLoading = useAuthStore((state) => state.isLoading);

  useEffect(() => {
    initSession();
  }, [initSession]);

  if (isLoading) {
    return <LoadingSpinner text="Restoring your CareerForge session..." />;
  }

  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <AppRouter />
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;