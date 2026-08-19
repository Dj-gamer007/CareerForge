import { Loader2 } from 'lucide-react';
import { cn } from '@/lib/utils';

export function LoadingSpinner({ className, text = 'Loading...' }: { className?: string; text?: string }) {
  return (
    <div className={cn('flex flex-col items-center justify-center p-12 text-slate-500', className)}>
      <Loader2 className="w-8 h-8 animate-spin text-indigo-600 mb-3" />
      <p className="text-sm font-medium">{text}</p>
    </div>
  );
}
