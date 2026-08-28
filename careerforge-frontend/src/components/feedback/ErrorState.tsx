import React from 'react';
import {
  AlertCircle,
  RotateCcw,
  WifiOff,
  ShieldAlert,
  FileQuestion,
  ServerCrash,
  AlertTriangle,
  Clock,
  ArrowLeft,
  LogIn,
  Home,
  Mail,
} from 'lucide-react';
import { Button } from '@/components/ui/Button';
import { Card, CardContent } from '@/components/ui/Card';
import { getErrorMessage } from '@/lib/utils';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/authStore';

export interface ActionConfig {
  label: string;
  onClick: () => void;
  icon?: React.ComponentType<{ className?: string }>;
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'destructive';
}

export interface ErrorStateProps {
  title?: string;
  message?: string;
  error?: unknown;
  primaryAction?: ActionConfig;
  onRetry?: () => void;
  retryText?: string;
  secondaryAction?: ActionConfig;
  variant?: 'card' | 'inline' | 'fullPage';
  className?: string;
}

export function ErrorState({
  title,
  message,
  error,
  primaryAction: customPrimaryAction,
  onRetry,
  retryText = 'Try Again',
  secondaryAction,
  variant = 'card',
  className = '',
}: ErrorStateProps) {
  const navigate = useNavigate();
  const { setDisabled, logout, user } = useAuthStore();

  const err = error as any;
  const status = err?.response?.status;
  const isAccountDisabled = status === 403 && err?.response?.data?.code === 'ACCOUNT_DISABLED';
  const isNetworkError =
    err?.message === 'Network Error' ||
    err?.code === 'ERR_NETWORK' ||
    err?.message?.includes('Network Error');

  // Semantic icon, title, description, and color scheme mapping
  let IconComponent = AlertCircle;
  let defaultTitle = 'Unable to Load Data';
  let defaultMessage = 'An unexpected error occurred while processing your request.';
  let badgeClasses = 'bg-rose-50 border-rose-100 text-rose-600';
  let defaultPrimaryAction: ActionConfig | undefined = onRetry
    ? { label: retryText, onClick: onRetry, icon: RotateCcw, variant: 'outline' }
    : undefined;
  let defaultSecondaryAction: ActionConfig | undefined = secondaryAction;

  if (isNetworkError) {
    IconComponent = WifiOff;
    defaultTitle = 'Connection Problem';
    defaultMessage = 'Unable to connect to the server. Please check your internet connection and try again.';
    badgeClasses = 'bg-indigo-50 border-indigo-100 text-indigo-600';
  } else if (status === 401) {
    IconComponent = ShieldAlert;
    defaultTitle = 'Session Expired';
    defaultMessage = 'Your session has expired. Please sign in again.';
    badgeClasses = 'bg-amber-50 border-amber-100 text-amber-600';
    if (!defaultPrimaryAction) {
      defaultPrimaryAction = {
        label: 'Sign In Again',
        onClick: () => {
          logout();
          navigate('/login');
        },
        icon: LogIn,
        variant: 'primary',
      };
    }
  } else if (isAccountDisabled) {
    IconComponent = ShieldAlert;
    defaultTitle = 'Account Disabled';
    defaultMessage =
      err?.response?.data?.message ||
      'Your account has been disabled by an administrator. Please contact support for assistance.';
    badgeClasses = 'bg-rose-50 border-rose-100 text-rose-600';
    if (!defaultPrimaryAction) {
      defaultPrimaryAction = {
        label: 'Back to Login',
        onClick: () => {
          setDisabled(false);
          logout();
          navigate('/login');
        },
        icon: ArrowLeft,
        variant: 'primary',
      };
    }
  } else if (status === 400) {
    IconComponent = AlertCircle;
    defaultTitle = 'Invalid Request';
    defaultMessage = 'The request could not be processed. Please check the details and try again.';
    badgeClasses = 'bg-amber-50 border-amber-100 text-amber-600';
  } else if (status === 403) {
    IconComponent = ShieldAlert;
    defaultTitle = 'Access Denied';
    defaultMessage = 'You do not have permission to perform this action or view this resource.';
    badgeClasses = 'bg-rose-50 border-rose-100 text-rose-600';
    if (!defaultSecondaryAction) {
      defaultSecondaryAction = {
        label: 'Return to Dashboard',
        onClick: () => {
          if (user?.role === 'ROLE_ADMIN') navigate('/admin/dashboard');
          else if (user?.role === 'ROLE_RECRUITER') navigate('/recruiter/dashboard');
          else if (user?.role === 'ROLE_STUDENT') navigate('/student/dashboard');
          else navigate('/');
        },
        icon: Home,
        variant: 'outline',
      };
    }
  } else if (status === 404) {
    IconComponent = FileQuestion;
    defaultTitle = 'Resource Not Found';
    defaultMessage = 'The requested resource could not be found or is no longer available.';
    badgeClasses = 'bg-slate-100 border-slate-200 text-slate-600';
  } else if (status === 409) {
    IconComponent = AlertTriangle;
    defaultTitle = 'Request Conflict';
    defaultMessage = 'This action conflicts with the current state of the record. Please refresh and try again.';
    badgeClasses = 'bg-amber-50 border-amber-100 text-amber-600';
  } else if (status === 422) {
    IconComponent = AlertCircle;
    defaultTitle = 'Validation Error';
    defaultMessage = 'The submitted data could not be processed. Please check your inputs and try again.';
    badgeClasses = 'bg-amber-50 border-amber-100 text-amber-600';
  } else if (status === 429) {
    IconComponent = Clock;
    defaultTitle = 'Too Many Requests';
    defaultMessage = 'Too many requests were made. Please slow down and try again shortly.';
    badgeClasses = 'bg-amber-50 border-amber-100 text-amber-600';
  } else if (status === 502 || status === 503) {
    IconComponent = ServerCrash;
    defaultTitle = 'Service Temporarily Unavailable';
    defaultMessage = 'CareerForge is temporarily unable to process your request. Please try again shortly.';
    badgeClasses = 'bg-rose-50 border-rose-100 text-rose-600';
  } else if (status === 504) {
    IconComponent = Clock;
    defaultTitle = 'Request Timeout';
    defaultMessage = 'The server took too long to respond. Please check your connection and try again.';
    badgeClasses = 'bg-amber-50 border-amber-100 text-amber-600';
  } else if (status && status >= 500) {
    IconComponent = ServerCrash;
    defaultTitle = 'Something Went Wrong';
    defaultMessage = 'An internal server error occurred. Please try again later.';
    badgeClasses = 'bg-rose-50 border-rose-100 text-rose-600';
  }

  const finalTitle = title || defaultTitle;
  const finalMessage = message || (error ? getErrorMessage(error, defaultMessage) : defaultMessage);
  const primaryAction = customPrimaryAction || (onRetry
    ? { label: retryText, onClick: onRetry, icon: RotateCcw, variant: 'outline' as const }
    : defaultPrimaryAction);
  const secondaryAct = secondaryAction || defaultSecondaryAction;

  // Inline Compact Variant (for modals, widgets, dashboard cards)
  if (variant === 'inline') {
    return (
      <div
        role="alert"
        className={`p-4 rounded-xl bg-white border border-slate-200/90 shadow-2xs text-center space-y-3 ${className}`}
      >
        <div className={`w-10 h-10 rounded-xl border flex items-center justify-center mx-auto ${badgeClasses}`}>
          <IconComponent className="w-5 h-5" />
        </div>
        <div className="space-y-1">
          <h4 className="text-sm font-bold text-slate-900 tracking-tight">{finalTitle}</h4>
          <p className="text-xs text-slate-500 max-w-xs mx-auto leading-relaxed">{finalMessage}</p>
        </div>
        {primaryAction && (
          <div className="pt-1">
            <Button
              size="sm"
              variant={primaryAction.variant || 'outline'}
              onClick={primaryAction.onClick}
              className="text-xs font-semibold h-8"
            >
              {primaryAction.icon && React.createElement(primaryAction.icon, { className: 'w-3.5 h-3.5 mr-1.5' })}
              {primaryAction.label}
            </Button>
          </div>
        )}
      </div>
    );
  }

  // Full-Page or Centered Card Variant
  const cardContent = (
    <Card className={`max-w-lg w-full shadow-md border-slate-200/90 rounded-2xl overflow-hidden bg-white ${className}`}>
      <CardContent className="p-6 sm:p-8 text-center space-y-5">
        {/* Status Indicator Icon */}
        <div className={`w-14 h-14 rounded-2xl border flex items-center justify-center mx-auto shadow-2xs ${badgeClasses}`}>
          <IconComponent className="w-7 h-7" />
        </div>

        {/* Title & Explanation */}
        <div className="space-y-1.5">
          <h3 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">{finalTitle}</h3>
          <p className="text-sm text-slate-600 max-w-md mx-auto leading-relaxed">{finalMessage}</p>
        </div>

        {/* Support hint if account disabled */}
        {isAccountDisabled && (
          <div className="bg-slate-50 border border-slate-200/80 rounded-xl p-3.5 text-left text-xs text-slate-600 space-y-1">
            <div className="flex items-center gap-1.5 font-bold text-slate-700">
              <Mail className="w-3.5 h-3.5 text-indigo-600" />
              <span>Contact Support</span>
            </div>
            <p className="text-slate-500">
              Please contact the CareerForge administration team at{' '}
              <a href="mailto:support@careerforge.local" className="font-semibold text-indigo-600 underline">
                support@careerforge.local
              </a>
              .
            </p>
          </div>
        )}

        {/* Action Buttons */}
        {(primaryAction || secondaryAct) && (
          <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-2.5">
            {primaryAction && (
              <Button
                size="sm"
                variant={primaryAction.variant || 'outline'}
                onClick={primaryAction.onClick}
                className="w-full sm:w-auto px-5 py-2 font-semibold text-xs shadow-2xs"
              >
                {primaryAction.icon && React.createElement(primaryAction.icon, { className: 'w-3.5 h-3.5 mr-1.5' })}
                {primaryAction.label}
              </Button>
            )}
            {secondaryAct && (
              <Button
                size="sm"
                variant={secondaryAct.variant || 'ghost'}
                onClick={secondaryAct.onClick}
                className="w-full sm:w-auto px-4 py-2 font-semibold text-xs shadow-2xs"
              >
                {secondaryAct.icon && React.createElement(secondaryAct.icon, { className: 'w-3.5 h-3.5 mr-1.5' })}
                {secondaryAct.label}
              </Button>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );

  if (variant === 'fullPage') {
    return <div className="min-h-[65vh] flex items-center justify-center p-4 sm:p-6 lg:p-8">{cardContent}</div>;
  }

  return <div className="my-8 flex justify-center px-4">{cardContent}</div>;
}
