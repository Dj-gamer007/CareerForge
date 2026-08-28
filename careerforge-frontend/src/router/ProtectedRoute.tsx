import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/authStore';
import { Role } from '@/types/auth.types';
import { ForbiddenState } from '@/components/feedback/ForbiddenState';
import { AccountDisabledState } from '@/components/feedback/AccountDisabledState';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';

export interface ProtectedRouteProps {
  allowedRoles?: Role[];
}

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, isAccountDisabled, user } = useAuthStore();
  const location = useLocation();

  if (isAccountDisabled) {
    return <AccountDisabledState />;
  }

  if (isLoading) {
    return <LoadingSpinner text="Checking authentication..." />;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <ForbiddenState />;
  }

  return <Outlet />;
}
