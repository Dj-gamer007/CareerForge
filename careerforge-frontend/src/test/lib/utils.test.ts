import { describe, it, expect } from 'vitest';
import { formatCurrency, formatDate, formatDateTime, parseDate, formatPercentage, formatFileSize, formatForDateTimeLocal, getErrorMessage } from '@/lib/utils';

describe('Format Utility Functions', () => {
  it('formats currency correctly', () => {
    expect(formatCurrency(500000, 'INR')).toContain('5,00,000');
    expect(formatCurrency(undefined)).toBe('N/A');
  });

  it('formats dates properly', () => {
    expect(formatDate('2026-08-19T12:00:00')).toContain('2026');
    expect(formatDate(undefined)).toBe('N/A');
  });

  it('parses and formats datetime with UTC normalization', () => {
    const parsed = parseDate('2026-08-23T13:06:00');
    expect(parsed).not.toBeNull();
    expect(formatDateTime('2026-08-23T13:06:00')).toContain('2026');
    expect(formatDateTime(undefined)).toBe('N/A');
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

  describe('Timezone Consistency & Conversion Tests', () => {
    it('converts UTC instant 2026-08-27T04:58:00Z to Aug 27, 2026, 10:28 AM in Asia/Kolkata (IST)', () => {
      // 04:58 UTC + 5:30 = 10:28 AM IST
      const result = formatDateTime('2026-08-27T04:58:00Z', 'Asia/Kolkata');
      expect(result).toBe('Aug 27, 2026, 10:28 AM');
    });

    it('normalizes backend local UTC representation 2026-08-27T04:58:00 to Aug 27, 2026, 10:28 AM in Asia/Kolkata', () => {
      const result = formatDateTime('2026-08-27T04:58:00', 'Asia/Kolkata');
      expect(result).toBe('Aug 27, 2026, 10:28 AM');
    });

    it('converts UTC instant to different timezones accurately', () => {
      // 04:58 UTC in America/New_York (EDT, UTC-4) is 12:58 AM
      expect(formatDateTime('2026-08-27T04:58:00Z', 'America/New_York')).toBe('Aug 27, 2026, 12:58 AM');

      // 04:58 UTC in Europe/London (BST, UTC+1) is 05:58 AM
      expect(formatDateTime('2026-08-27T04:58:00Z', 'Europe/London')).toBe('Aug 27, 2026, 05:58 AM');

      // 04:58 UTC in Asia/Tokyo (JST, UTC+9) is 01:58 PM
      expect(formatDateTime('2026-08-27T04:58:00Z', 'Asia/Tokyo')).toBe('Aug 27, 2026, 01:58 PM');
    });

    it('preserves date-only strings without cross-timezone day shifting', () => {
      expect(formatDate('2026-08-27')).toBe('Aug 27, 2026');
      expect(formatDate('2026-08-27', 'America/New_York')).toBe('Aug 27, 2026');
      expect(formatDate('2026-08-27', 'Asia/Kolkata')).toBe('Aug 27, 2026');
    });

    it('formats date and time for datetime-local input pre-population', () => {
      // When pre-populating datetime-local input from a date
      const localStr = formatForDateTimeLocal('2026-08-27T04:58:00Z');
      expect(localStr).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/);
    });
  });

  describe('Centralized API Error Message Extraction', () => {
    it('extracts safe backend message', () => {
      const error = { response: { status: 400, data: { message: 'Invalid skill selection' } } };
      expect(getErrorMessage(error)).toBe('Invalid skill selection');
    });

    it('maps 401 to session expired', () => {
      const error = { response: { status: 401, data: {} } };
      expect(getErrorMessage(error)).toBe('Your session has expired. Please sign in again.');
    });

    it('maps 403 ACCOUNT_DISABLED to support guidance', () => {
      const error = { response: { status: 403, data: { code: 'ACCOUNT_DISABLED' } } };
      expect(getErrorMessage(error)).toContain('disabled by an administrator');
    });

    it('maps 403 permission error', () => {
      const error = { response: { status: 403, data: {} } };
      expect(getErrorMessage(error)).toBe('You do not have permission to perform this action.');
    });

    it('maps 404 resource not found', () => {
      const error = { response: { status: 404, data: {} } };
      expect(getErrorMessage(error)).toBe('The requested resource could not be found.');
    });

    it('maps 409 conflict', () => {
      const error = { response: { status: 409, data: {} } };
      expect(getErrorMessage(error)).toBe('Your request conflicts with the current data. Please refresh and try again.');
    });

    it('maps 500 server error', () => {
      const error = { response: { status: 500, data: {} } };
      expect(getErrorMessage(error)).toBe('Something went wrong on the server. Please try again later.');
    });

    it('maps 502 gateway error', () => {
      const error = { response: { status: 502, data: {} } };
      expect(getErrorMessage(error)).toBe('The server is temporarily unavailable. Please try again shortly.');
    });

    it('maps network errors gracefully', () => {
      const error = { message: 'Network Error', code: 'ERR_NETWORK' };
      expect(getErrorMessage(error)).toBe('Unable to connect to CareerForge. Please check your connection.');
    });
  });
});
