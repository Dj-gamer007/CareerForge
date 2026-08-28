import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, LoginFormData } from '@/schemas/auth.schema';
import { authService } from '@/services/auth.service';
import { useAuthStore } from '@/features/auth/authStore';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { AlertCircle } from 'lucide-react';

export function LoginPage() {
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const setAuth = useAuthStore((state) => state.setAuth);
  const navigate = useNavigate();
  const location = useLocation();

  const from = (location.state as any)?.from?.pathname || null;

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    try {
      setIsLoading(true);
      setErrorMsg(null);
      const res = await authService.login(data);
      setAuth(res.accessToken, res.refreshToken, res.user);

      if (from) {
        navigate(from, { replace: true });
        return;
      }

      if (res.user.role === 'ROLE_ADMIN') navigate('/admin/dashboard');
      else if (res.user.role === 'ROLE_RECRUITER') navigate('/recruiter/dashboard');
      else navigate('/student/dashboard');
    } catch (err: any) {
      if (!err.response) {
        setErrorMsg('Unable to connect to CareerForge. Please check your connection.');
      } else if (err.response.status === 401) {
        setErrorMsg(err.response.data?.message || 'Invalid email or password. Please try again.');
      } else if (err.response.status === 403) {
        if (err.response.data?.code === 'ACCOUNT_DISABLED') {
          setErrorMsg('Your account has been disabled by an administrator. Please contact support for assistance.');
        } else {
          setErrorMsg(err.response.data?.message || 'Access denied. You do not have permission to sign in.');
        }
      } else if (err.response.status === 429) {
        setErrorMsg('Too many login attempts. Please wait a moment and try again.');
      } else if (err.response.status >= 500) {
        if (err.response.status === 502 || err.response.status === 503) {
          setErrorMsg('The server is temporarily unavailable. Please try again shortly.');
        } else if (err.response.status === 504) {
          setErrorMsg('The server took too long to respond. Please try again.');
        } else {
          setErrorMsg('Something went wrong on the server. Please try again later.');
        }
      } else {
        setErrorMsg(err.response.data?.message || 'Authentication failed. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      <div className="text-center mb-6">
        <h2 className="text-xl font-bold text-slate-900">Welcome back</h2>
        <p className="text-sm text-slate-500 mt-1">Sign in to access your CareerForge account</p>
      </div>

      {errorMsg && (
        <div className="mb-4 p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-center gap-2 text-rose-700 text-xs">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Input
          label="Email Address"
          type="email"
          placeholder="you@careerforge.local"
          error={errors.email?.message}
          {...register('email')}
        />

        <Input
          label="Password"
          type="password"
          placeholder="••••••••"
          error={errors.password?.message}
          {...register('password')}
        />

        <Button type="submit" className="w-full mt-2" isLoading={isLoading}>
          Sign In
        </Button>
      </form>

      <div className="mt-6 text-center text-xs text-slate-500">
        Don't have an account?{' '}
        <Link to="/register" className="font-semibold text-indigo-600 hover:text-indigo-700">
          Create an account
        </Link>
      </div>
    </div>
  );
}
