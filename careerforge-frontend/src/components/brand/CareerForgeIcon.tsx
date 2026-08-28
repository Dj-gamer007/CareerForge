import React from 'react';
import { cn } from '@/lib/utils';

interface CareerForgeIconProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  className?: string;
}

/**
 * Master CareerForge Brand Icon
 * Single source of truth rendering the exact official CareerForge logo image.
 */
export function CareerForgeIcon({ className, ...props }: CareerForgeIconProps) {
  return (
    <img
      src="/careerforge-logo.png"
      alt="CareerForge"
      className={cn('w-9 h-9 rounded-xl object-contain shrink-0 shadow-sm shadow-indigo-200', className)}
      {...props}
    />
  );
}
