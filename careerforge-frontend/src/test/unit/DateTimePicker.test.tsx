import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { DateTimePicker } from '@/components/ui/DateTimePicker';

describe('DateTimePicker Component', () => {
  it('renders date, hour, minute, and period selects', () => {
    const onChange = vi.fn();
    render(<DateTimePicker label="Interview Date & Time" onChange={onChange} />);

    expect(screen.getByText('Interview Date & Time')).toBeInTheDocument();
    expect(screen.getByLabelText('Interview Date')).toBeInTheDocument();
    expect(screen.getByLabelText('Hour')).toBeInTheDocument();
    expect(screen.getByLabelText('Minute')).toBeInTheDocument();
    expect(screen.getByLabelText('Period')).toBeInTheDocument();
  });

  it('correctly calculates 10:30 AM in ISO format when user selects date and time', () => {
    const onChange = vi.fn();
    render(<DateTimePicker onChange={onChange} />);

    const dateInput = screen.getByLabelText('Interview Date');
    const hourSelect = screen.getByLabelText('Hour');
    const minuteSelect = screen.getByLabelText('Minute');
    const periodSelect = screen.getByLabelText('Period');

    fireEvent.change(dateInput, { target: { value: '2026-08-30' } });
    fireEvent.change(hourSelect, { target: { value: '10' } });
    fireEvent.change(minuteSelect, { target: { value: '30' } });
    fireEvent.change(periodSelect, { target: { value: 'AM' } });

    expect(onChange).toHaveBeenCalled();
    const lastCallArg = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    const parsedDate = new Date(lastCallArg);
    expect(parsedDate.getFullYear()).toBe(2026);
    expect(parsedDate.getMonth()).toBe(7); // August (0-indexed)
    expect(parsedDate.getDate()).toBe(30);
    expect(parsedDate.getHours()).toBe(10);
    expect(parsedDate.getMinutes()).toBe(30);
  });

  it('correctly calculates 10:30 PM (22:30)', () => {
    const onChange = vi.fn();
    render(<DateTimePicker onChange={onChange} />);

    const dateInput = screen.getByLabelText('Interview Date');
    const hourSelect = screen.getByLabelText('Hour');
    const minuteSelect = screen.getByLabelText('Minute');
    const periodSelect = screen.getByLabelText('Period');

    fireEvent.change(dateInput, { target: { value: '2026-08-30' } });
    fireEvent.change(hourSelect, { target: { value: '10' } });
    fireEvent.change(minuteSelect, { target: { value: '30' } });
    fireEvent.change(periodSelect, { target: { value: 'PM' } });

    const lastCallArg = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    const parsedDate = new Date(lastCallArg);
    expect(parsedDate.getHours()).toBe(22);
    expect(parsedDate.getMinutes()).toBe(30);
  });

  it('handles 12:00 AM (midnight -> 0 hours)', () => {
    const onChange = vi.fn();
    render(<DateTimePicker onChange={onChange} />);

    const dateInput = screen.getByLabelText('Interview Date');
    const hourSelect = screen.getByLabelText('Hour');
    const minuteSelect = screen.getByLabelText('Minute');
    const periodSelect = screen.getByLabelText('Period');

    fireEvent.change(dateInput, { target: { value: '2026-08-30' } });
    fireEvent.change(hourSelect, { target: { value: '12' } });
    fireEvent.change(minuteSelect, { target: { value: '00' } });
    fireEvent.change(periodSelect, { target: { value: 'AM' } });

    const lastCallArg = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    const parsedDate = new Date(lastCallArg);
    expect(parsedDate.getHours()).toBe(0);
    expect(parsedDate.getMinutes()).toBe(0);
  });

  it('handles 12:00 PM (noon -> 12 hours)', () => {
    const onChange = vi.fn();
    render(<DateTimePicker onChange={onChange} />);

    const dateInput = screen.getByLabelText('Interview Date');
    const hourSelect = screen.getByLabelText('Hour');
    const minuteSelect = screen.getByLabelText('Minute');
    const periodSelect = screen.getByLabelText('Period');

    fireEvent.change(dateInput, { target: { value: '2026-08-30' } });
    fireEvent.change(hourSelect, { target: { value: '12' } });
    fireEvent.change(minuteSelect, { target: { value: '00' } });
    fireEvent.change(periodSelect, { target: { value: 'PM' } });

    const lastCallArg = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    const parsedDate = new Date(lastCallArg);
    expect(parsedDate.getHours()).toBe(12);
    expect(parsedDate.getMinutes()).toBe(0);
  });

  it('correctly calculates Aug 28, 2026, 10:20 AM and displays preview with year 2026', () => {
    const onChange = vi.fn();
    const { rerender } = render(<DateTimePicker onChange={onChange} />);

    const dateInput = screen.getByLabelText('Interview Date');
    const hourSelect = screen.getByLabelText('Hour');
    const minuteSelect = screen.getByLabelText('Minute');
    const periodSelect = screen.getByLabelText('Period');

    fireEvent.change(dateInput, { target: { value: '2026-08-28' } });
    fireEvent.change(hourSelect, { target: { value: '10' } });
    fireEvent.change(minuteSelect, { target: { value: '20' } });
    fireEvent.change(periodSelect, { target: { value: 'AM' } });

    const lastCallArg = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    const parsedDate = new Date(lastCallArg);
    expect(parsedDate.getFullYear()).toBe(2026);
    expect(parsedDate.getMonth()).toBe(7); // Aug
    expect(parsedDate.getDate()).toBe(28);
    expect(parsedDate.getHours()).toBe(10);
    expect(parsedDate.getMinutes()).toBe(20);

    // Pass the calculated ISO string back as value prop to test UI binding & preview
    rerender(<DateTimePicker value={lastCallArg} onChange={onChange} />);
    expect(dateInput).toHaveValue('2026-08-28');
    expect(hourSelect).toHaveValue('10');
    expect(minuteSelect).toHaveValue('20');
    expect(periodSelect).toHaveValue('AM');
    expect(screen.getByText(/Aug 28, 2026/)).toBeInTheDocument();
  });

  it('correctly calculates 1:05 PM (13:05)', () => {
    const onChange = vi.fn();
    render(<DateTimePicker onChange={onChange} />);

    const dateInput = screen.getByLabelText('Interview Date');
    const hourSelect = screen.getByLabelText('Hour');
    const minuteSelect = screen.getByLabelText('Minute');
    const periodSelect = screen.getByLabelText('Period');

    fireEvent.change(dateInput, { target: { value: '2026-08-28' } });
    fireEvent.change(hourSelect, { target: { value: '1' } });
    fireEvent.change(minuteSelect, { target: { value: '05' } });
    fireEvent.change(periodSelect, { target: { value: 'PM' } });

    const lastCallArg = onChange.mock.calls[onChange.mock.calls.length - 1][0];
    const parsedDate = new Date(lastCallArg);
    expect(parsedDate.getHours()).toBe(13);
    expect(parsedDate.getMinutes()).toBe(5);
  });
});
