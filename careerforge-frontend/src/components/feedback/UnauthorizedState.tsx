import { LogIn } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { useNavigate } from 'react-router-dom';

export function UnauthorizedState({ message = 'You need to be signed in to access this page.' }: { message?: string }) {
  const navigate = useNavigate();

  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center p-6 text-center">
      <div className="w-16 h-16 rounded-full bg-indigo-50 flex items-center justify-center text-indigo-600 mb-6">
        <LogIn className="w-8 h-8" />
      </div>
      <h2 className="text-2xl font-bold text-slate-900 mb-2">Authentication Required</h2>
      <p className="text-sm text-slate-500 mb-8 max-w-md">{message}</p>
      <Button onClick={() => navigate('/login')}>
        Sign In to Continue
      </Button>
    </div>
  );
}
