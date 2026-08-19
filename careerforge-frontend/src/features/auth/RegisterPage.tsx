import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { registerSchema, RegisterFormData } from '@/schemas/auth.schema';
import { authService } from '@/services/auth.service';
import { useAuthStore } from '@/features/auth/authStore';
import { Link, useNavigate } from 'react-router-dom';
import { Input } from '@/components/ui/Input';
import { Select } from '@/components/ui/Select';
import { Button } from '@/components/ui/Button';
import { AlertCircle } from 'lucide-react';

export function RegisterPage() {
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const setAuth = useAuthStore((state) => state.setAuth);
  const navigate = useNavigate();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      role: 'ROLE_STUDENT',
    },
  });

  const onSubmit = async (data: RegisterFormData) => {
    try {
      setIsLoading(true);
      setErrorMsg(null);
      const res = await authService.register({
        email: data.email,
        password: data.password,
        role: data.role,
      });
      setAuth(res.accessToken, res.refreshToken, res.user);

      if (res.user.role === 'ROLE_RECRUITER') navigate('/recruiter/company');
      else navigate('/student/profile');
    } catch (err: any) {
      setErrorMsg(err.response?.data?.message || 'Registration failed. Please verify your details.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div>
      <div className="text-center mb-6">
        <h2 className="text-xl font-bold text-slate-900">Create your account</h2>
        <p className="text-sm text-slate-500 mt-1">Join CareerForge as a candidate or hiring recruiter</p>
      </div>

      {errorMsg && (
        <div className="mb-4 p-3 bg-rose-50 border border-rose-200 rounded-lg flex items-center gap-2 text-rose-700 text-xs">
          <AlertCircle className="w-4 h-4 shrink-0" />
          <span>{errorMsg}</span>
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <Select
          label="I am joining as a:"
          error={errors.role?.message}
          {...register('role')}
        >
          <option value="ROLE_STUDENT">Job Seeker / Student</option>
          <option value="ROLE_RECRUITER">Recruiter / Employer</option>
        </Select>

        <Input
          label="Email Address"
          type="email"
          placeholder="you@domain.com"
          error={errors.email?.message}
          {...register('email')}
        />

        <Input
          label="Password"
          type="password"
          placeholder="Min 8 chars with uppercase, number & symbol"
          error={errors.password?.message}
          {...register('password')}
        />

        <Input
          label="Confirm Password"
          type="password"
          placeholder="Re-enter password"
          error={errors.confirmPassword?.message}
          {...register('confirmPassword')}
        />

        <Button type="submit" className="w-full mt-2" isLoading={isLoading}>
          Create Account
        </Button>
      </form>

      <div className="mt-6 text-center text-xs text-slate-500">
        Already have an account?{' '}
        <Link to="/login" className="font-semibold text-indigo-600 hover:text-indigo-700">
          Sign In
        </Link>
      </div>
    </div>
  );
}
