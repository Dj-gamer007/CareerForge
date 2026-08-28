import { ShieldAlert, Mail, ArrowLeft, LogOut } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/authStore';
import { CareerForgeIcon } from '@/components/brand/CareerForgeIcon';

export function AccountDisabledState() {
  const navigate = useNavigate();
  const { disabledMessage, setDisabled, logout, user } = useAuthStore();

  const handleReturnToLogin = () => {
    setDisabled(false);
    logout();
    navigate('/login');
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-between">
      {/* Top Navigation Header */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-20 shadow-2xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <Link to="/" className="flex items-center gap-2.5 font-bold text-xl text-slate-900 tracking-tight">
            <CareerForgeIcon />
            <span>Career<span className="text-indigo-600">Forge</span></span>
          </Link>

          <div className="flex items-center gap-3">
            {user?.email && (
              <span className="hidden sm:inline text-xs text-slate-400 font-medium">
                {user.email}
              </span>
            )}
            <Button
              variant="outline"
              size="sm"
              onClick={handleReturnToLogin}
              className="text-xs font-semibold"
            >
              <LogOut className="w-3.5 h-3.5 mr-1.5 text-slate-500" />
              Sign Out
            </Button>
          </div>
        </div>
      </header>

      {/* Centered Feedback Section */}
      <main className="flex-1 flex items-center justify-center p-4 sm:p-6 lg:p-8">
        <Card className="max-w-lg w-full shadow-md border-slate-200/90 rounded-2xl overflow-hidden bg-white">
          <CardContent className="p-6 sm:p-8 text-center space-y-6">
            {/* Status Indicator Icon */}
            <div className="w-16 h-16 rounded-2xl bg-rose-50 border border-rose-100 flex items-center justify-center mx-auto shadow-xs text-rose-600">
              <ShieldAlert className="w-8 h-8" />
            </div>

            {/* Title & Explanation */}
            <div className="space-y-2">
              <h1 className="text-2xl font-extrabold text-slate-900 tracking-tight">
                Account Disabled
              </h1>
              <p className="text-sm text-slate-600 leading-relaxed">
                {disabledMessage ||
                  'Your account has been disabled by an administrator. Access to the CareerForge portal is currently suspended.'}
              </p>
            </div>

            {/* Support Information Box */}
            <div className="bg-slate-50/80 border border-slate-200/80 rounded-xl p-4 text-left space-y-1.5">
              <div className="flex items-center gap-2 text-xs font-bold text-slate-700">
                <Mail className="w-4 h-4 text-indigo-600 shrink-0" />
                <span>Support & Account Inquiries</span>
              </div>
              <p className="text-xs text-slate-500 leading-relaxed">
                If you believe this administrative action was taken in error or require further clarification, please contact our support team at{' '}
                <a
                  href="mailto:support@careerforge.local"
                  className="font-semibold text-indigo-600 hover:text-indigo-700 underline"
                >
                  support@careerforge.local
                </a>
                .
              </p>
            </div>

            {/* Action Button */}
            <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
              <Button
                onClick={handleReturnToLogin}
                variant="primary"
                className="w-full sm:w-auto px-8 py-2.5 shadow-sm font-semibold text-sm"
              >
                <ArrowLeft className="w-4 h-4 mr-2" />
                Back to Login
              </Button>
            </div>
          </CardContent>
        </Card>
      </main>

      {/* Footer */}
      <footer className="py-4 text-center text-xs text-slate-400 border-t border-slate-200/60 bg-white">
        © {new Date().getFullYear()} CareerForge Platform. All rights reserved.
      </footer>
    </div>
  );
}
