import React, { useEffect, useState } from 'react';
import { Clock } from 'lucide-react';
import { formatDateTime, parseDate } from '@/lib/utils';

interface DateTimePickerProps {
  label?: string;
  value?: string;
  onChange: (isoString: string) => void;
  minDate?: string;
  className?: string;
}

export const DateTimePicker: React.FC<DateTimePickerProps> = ({
  label = 'Interview Date & Time',
  value,
  onChange,
  minDate,
  className = '',
}) => {
  // Local state for parts
  const [date, setDate] = useState<string>('');
  const [hour, setHour] = useState<string>('10');
  const [minute, setMinute] = useState<string>('00');
  const [period, setPeriod] = useState<'AM' | 'PM'>('AM');

  // Parse incoming value on mount or change
  useEffect(() => {
    if (!value) return;
    try {
      const d = parseDate(value) || new Date(value);
      if (!isNaN(d.getTime())) {
        const yyyy = d.getFullYear();
        if (yyyy >= 1900) {
          const mm = String(d.getMonth() + 1).padStart(2, '0');
          const dd = String(d.getDate()).padStart(2, '0');
          setDate(`${yyyy}-${mm}-${dd}`);

          let hours = d.getHours();
          const p: 'AM' | 'PM' = hours >= 12 ? 'PM' : 'AM';
          hours = hours % 12 || 12;
          setHour(String(hours));

          const mins = (Math.round(d.getMinutes() / 5) * 5) % 60;
          setMinute(String(mins).padStart(2, '0'));
          setPeriod(p);
        }
      }
    } catch {
      // Ignore invalid initial value
    }
  }, [value]);

  // Combine and emit ISO string whenever any part changes
  const updateComposite = (
    newDate: string,
    newHour: string,
    newMinute: string,
    newPeriod: 'AM' | 'PM'
  ) => {
    if (!newDate) {
      onChange('');
      return;
    }

    const parts = newDate.split('-').map((v) => parseInt(v, 10));
    if (parts.length !== 3) return;
    const [yyyy, mm, dd] = parts;

    // Guard against incomplete 2-digit typing or invalid year
    if (!yyyy || yyyy < 1900 || isNaN(mm) || isNaN(dd)) {
      return;
    }

    let h = parseInt(newHour, 10) || 12;
    if (newPeriod === 'PM' && h < 12) h += 12;
    if (newPeriod === 'AM' && h === 12) h = 0;

    const m = parseInt(newMinute, 10) || 0;

    const localDate = new Date();
    localDate.setFullYear(yyyy, mm - 1, dd);
    localDate.setHours(h, m, 0, 0);

    if (!isNaN(localDate.getTime())) {
      onChange(localDate.toISOString());
    }
  };

  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = e.target.value;
    setDate(val);
    updateComposite(val, hour, minute, period);
  };

  const handleHourChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    setHour(val);
    updateComposite(date, val, minute, period);
  };

  const handleMinuteChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value;
    setMinute(val);
    updateComposite(date, hour, val, period);
  };

  const handlePeriodChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const val = e.target.value as 'AM' | 'PM';
    setPeriod(val);
    updateComposite(date, hour, minute, val);
  };

  const minutesOptions = [
    '00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55',
  ];

  return (
    <div className={`space-y-2 ${className}`}>
      {label && <label className="block text-xs font-semibold text-slate-700">{label}</label>}

      <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2">
        {/* Date Selector */}
        <div className="flex-1 min-w-[150px]">
          <input
            type="date"
            min={minDate || new Date().toISOString().split('T')[0]}
            value={date}
            onChange={handleDateChange}
            className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-primary-500 focus:outline-none"
            aria-label="Interview Date"
          />
        </div>

        {/* Time Controls */}
        <div className="flex items-center gap-1.5 shrink-0 justify-between sm:justify-start">
          <select
            value={hour}
            onChange={handleHourChange}
            className="w-16 px-2 py-2 text-sm font-medium border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-primary-500 focus:outline-none"
            aria-label="Hour"
          >
            {Array.from({ length: 12 }, (_, i) => i + 1).map((h) => (
              <option key={h} value={String(h)}>
                {String(h).padStart(2, '0')}
              </option>
            ))}
          </select>

          <span className="text-slate-400 font-bold">:</span>

          <select
            value={minute}
            onChange={handleMinuteChange}
            className="w-16 px-2 py-2 text-sm font-medium border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-primary-500 focus:outline-none"
            aria-label="Minute"
          >
            {minutesOptions.map((m) => (
              <option key={m} value={m}>
                {m}
              </option>
            ))}
          </select>

          <select
            value={period}
            onChange={handlePeriodChange}
            className="w-20 px-2 py-2 text-sm font-semibold border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-primary-500 focus:outline-none"
            aria-label="Period"
          >
            <option value="AM">AM</option>
            <option value="PM">PM</option>
          </select>
        </div>
      </div>

      {/* Live Preview */}
      {value && date && (
        <div className="flex items-center gap-1.5 text-xs text-primary-700 bg-primary-50 px-2.5 py-1.5 rounded border border-primary-200">
          <Clock className="w-3.5 h-3.5 shrink-0" />
          <span>Scheduled for: <strong>{formatDateTime(value)}</strong></span>
        </div>
      )}
    </div>
  );
};
