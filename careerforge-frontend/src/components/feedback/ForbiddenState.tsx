import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/authStore';
import { ErrorState } from '@/components/feedback/ErrorState';
import { Home } from 'lucide-react';

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
    <ErrorState
      variant="fullPage"
      title="Access Denied"
      message={`You do not have permission to view this resource. Your account role (${
        user?.role ? user.role.replace('ROLE_', '').toLowerCase() : 'Guest'
      }) lacks the required privileges.`}
      secondaryAction={{
        label: 'Return to Dashboard',
        onClick: () => navigate(getDashboardPath()),
        icon: Home,
        variant: 'primary',
      }}
    />
  );
}
