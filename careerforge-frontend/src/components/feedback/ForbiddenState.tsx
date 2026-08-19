import { ShieldAlert } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/authStore';

export function ForbiddenState() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);

  const getDashboardPath = () => {
    if (user?.role === 'ROLE_ADMIN') return '/admin/dashboard';
    if (user?.role === 'ROLE_RECRUITER') return '/recruiter/dashboard';
    if (user?.role === 'ROLE_STUDENT') return '/student/dashboard';
    return '/';
  };

  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center p-6 text-center">
      <div className="w-16 h-16 rounded-full bg-rose-50 flex items-center justify-center text-rose-600 mb-6">
        <ShieldAlert className="w-8 h-8" />
      </div>
      <h2 className="text-2xl font-bold text-slate-900 mb-2">Access Denied (403 Forbidden)</h2>
      <p className="text-sm text-slate-500 mb-8 max-w-md">
        You do not have permission to view this resource. Your account role ({user?.role || 'Guest'}) lacks the required privileges.
      </p>
      <Button onClick={() => navigate(getDashboardPath())}>
        Return to Dashboard
      </Button>
    </div>
  );
}
