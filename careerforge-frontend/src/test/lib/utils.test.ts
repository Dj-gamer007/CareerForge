import { describe, it, expect } from 'vitest';
import { formatCurrency, formatDate, formatPercentage, formatFileSize } from '@/lib/utils';

describe('Format Utility Functions', () => {
  it('formats currency correctly', () => {
    expect(formatCurrency(500000, 'INR')).toContain('5,00,000');
    expect(formatCurrency(undefined)).toBe('N/A');
  });

  it('formats dates properly', () => {
    expect(formatDate('2026-08-19T12:00:00')).toContain('2026');
    expect(formatDate(undefined)).toBe('N/A');
  });

  it('formats percentages properly', () => {
    expect(formatPercentage(85.456)).toBe('85.5%');
    expect(formatPercentage(undefined)).toBe('0%');
  });

  it('formats file sizes properly', () => {
    expect(formatFileSize(500)).toBe('500 B');
    expect(formatFileSize(2048)).toBe('2.0 KB');
    expect(formatFileSize(1048576 * 2.5)).toBe('2.50 MB');
  });
});
