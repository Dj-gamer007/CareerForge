import { cn } from '@/lib/utils';

export interface ScoreGaugeProps {
  score: number;
  size?: 'sm' | 'md' | 'lg';
  showLabel?: boolean;
}

export function ScoreGauge({ score, size = 'md', showLabel = true }: ScoreGaugeProps) {
  const normalizedScore = Math.min(100, Math.max(0, score || 0));

  let colorClass = 'text-emerald-600 border-emerald-500 bg-emerald-50';
  let badgeColor = 'bg-emerald-100 text-emerald-800';

  if (normalizedScore < 50) {
    colorClass = 'text-rose-600 border-rose-500 bg-rose-50';
    badgeColor = 'bg-rose-100 text-rose-800';
  } else if (normalizedScore < 75) {
    colorClass = 'text-amber-600 border-amber-500 bg-amber-50';
    badgeColor = 'bg-amber-100 text-amber-800';
  }

  const sizes = {
    sm: 'w-10 h-10 text-xs font-bold',
    md: 'w-14 h-14 text-sm font-bold',
    lg: 'w-20 h-20 text-lg font-extrabold',
  };

  return (
    <div className="inline-flex items-center gap-2">
      <div
        className={cn(
          'rounded-full flex items-center justify-center border-2 transition-all',
          sizes[size],
          colorClass
        )}
      >
        <span>{normalizedScore.toFixed(0)}%</span>
      </div>
      {showLabel && (
        <span className={cn('text-xs px-2 py-0.5 rounded-full font-medium', badgeColor)}>
          {normalizedScore >= 75 ? 'Strong Match' : normalizedScore >= 50 ? 'Moderate Match' : 'Low Match'}
        </span>
      )}
    </div>
  );
}
