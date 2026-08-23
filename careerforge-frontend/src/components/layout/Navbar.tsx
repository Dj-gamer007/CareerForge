import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/authStore';
import { NotificationBell } from '@/features/notifications/NotificationBell';
import { Button } from '@/components/ui/Button';
import { Briefcase, User, LogOut } from 'lucide-react';
import { authService } from '@/services/auth.service';

export function Navbar() {
  const { user, isAuthenticated, logout, refreshToken } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      if (refreshToken) {
        await authService.logout(refreshToken);
      }
    } catch {
      // Ignore network errors on logout
    } finally {
      logout();
      navigate('/login');
    }
  };

  const getDashboardLink = () => {
    if (user?.role === 'ROLE_ADMIN') return '/admin/dashboard';
    if (user?.role === 'ROLE_RECRUITER') return '/recruiter/dashboard';
    if (user?.role === 'ROLE_STUDENT') return '/student/dashboard';
    return '/';
  };

  const getProfileLink = () => {
    if (user?.role === 'ROLE_RECRUITER') return '/recruiter/profile';
    if (user?.role === 'ROLE_STUDENT') return '/student/profile';
    return '#';
  };

  return (
    <header className="sticky top-0 z-30 bg-white/95 backdrop-blur-sm border-b border-slate-200 shadow-xs">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center gap-8">
          <Link to="/" className="flex items-center gap-2.5 font-bold text-xl text-slate-900 tracking-tight">
            <div className="w-9 h-9 rounded-xl bg-indigo-600 flex items-center justify-center text-white shadow-sm shadow-indigo-200">
              <Briefcase className="w-5 h-5" />
            </div>
            <span>Career<span className="text-indigo-600">Forge</span></span>
          </Link>

          {/* Navigation links */}
          <nav className="hidden md:flex items-center gap-6 text-sm font-medium text-slate-600">
            {user?.role !== 'ROLE_RECRUITER' && (
              <>
                <Link to="/jobs" className="hover:text-indigo-600 transition-colors">
                  Find Jobs
                </Link>
                <Link to="/companies" className="hover:text-indigo-600 transition-colors">
                  Companies
                </Link>
              </>
            )}
            {isAuthenticated && (
              <Link to={getDashboardLink()} className="hover:text-indigo-600 transition-colors">
                Dashboard
              </Link>
            )}
          </nav>
        </div>

        {/* Right action bar */}
        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <>
              <NotificationBell />
              <Link
                to={getProfileLink()}
                className="hidden sm:flex items-center gap-2 pl-3 border-l border-slate-200 hover:opacity-80 transition-opacity"
                title="View Profile"
              >
                <div className="w-8 h-8 rounded-full bg-slate-100 flex items-center justify-center text-slate-600 text-xs font-bold border border-slate-200">
                  <User className="w-4 h-4" />
                </div>
                <div className="text-left text-xs">
                  <p className="font-semibold text-slate-900 truncate max-w-[130px]">{user?.email}</p>
                  <p className="text-slate-400 capitalize">{user?.role?.replace('ROLE_', '').toLowerCase()}</p>
                </div>
              </Link>
              <Button variant="ghost" size="sm" onClick={handleLogout} title="Sign Out">
                <LogOut className="w-4 h-4 text-slate-600" />
              </Button>
            </>
          ) : (
            <div className="flex items-center gap-2">
              <Button variant="ghost" size="sm" onClick={() => navigate('/login')}>
                Sign In
              </Button>
              <Button size="sm" onClick={() => navigate('/register')}>
                Get Started
              </Button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
