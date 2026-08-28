import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ErrorState } from '@/components/feedback/ErrorState';
import { MemoryRouter } from 'react-router-dom';

describe('Unified ErrorState Component', () => {
  it('renders network connection error correctly', () => {
    const onRetry = vi.fn();
    render(
      <MemoryRouter>
        <ErrorState
          error={{ message: 'Network Error', code: 'ERR_NETWORK' }}
          onRetry={onRetry}
        />
      </MemoryRouter>
    );

    expect(screen.getByText('Connection Problem')).toBeInTheDocument();
    expect(
      screen.getByText('Unable to connect to CareerForge. Please check your connection.')
    ).toBeInTheDocument();

    const retryBtn = screen.getByRole('button', { name: /try again/i });
    expect(retryBtn).toBeInTheDocument();
    fireEvent.click(retryBtn);
    expect(onRetry).toHaveBeenCalledTimes(1);
  });

  it('renders 401 Session Expired with Sign In action', () => {
    render(
      <MemoryRouter>
        <ErrorState error={{ response: { status: 401 } }} />
      </MemoryRouter>
    );

    expect(screen.getByText('Session Expired')).toBeInTheDocument();
    expect(screen.getByText('Your session has expired. Please sign in again.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in again/i })).toBeInTheDocument();
  });

  it('renders 403 Access Denied with Dashboard return action', () => {
    render(
      <MemoryRouter>
        <ErrorState error={{ response: { status: 403 } }} />
      </MemoryRouter>
    );

    expect(screen.getByText('Access Denied')).toBeInTheDocument();
    expect(
      screen.getByText('You do not have permission to perform this action.')
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /return to dashboard/i })).toBeInTheDocument();
  });

  it('renders 403 ACCOUNT_DISABLED with Back to Login and support contact', () => {
    render(
      <MemoryRouter>
        <ErrorState
          error={{
            response: {
              status: 403,
              data: { code: 'ACCOUNT_DISABLED', message: 'Your account has been administratively disabled.' },
            },
          }}
        />
      </MemoryRouter>
    );

    expect(screen.getByText('Account Disabled')).toBeInTheDocument();
    expect(screen.getByText('Your account has been administratively disabled.')).toBeInTheDocument();
    expect(screen.getByText(/support@careerforge.local/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /back to login/i })).toBeInTheDocument();
  });

  it('renders 404 Resource Not Found', () => {
    render(
      <MemoryRouter>
        <ErrorState error={{ response: { status: 404 } }} />
      </MemoryRouter>
    );

    expect(screen.getByText('Resource Not Found')).toBeInTheDocument();
    expect(
      screen.getByText('The requested resource could not be found.')
    ).toBeInTheDocument();
  });

  it('renders 409 Request Conflict', () => {
    render(
      <MemoryRouter>
        <ErrorState error={{ response: { status: 409 } }} />
      </MemoryRouter>
    );

    expect(screen.getByText('Request Conflict')).toBeInTheDocument();
  });

  it('renders 422 Validation Error', () => {
    render(
      <MemoryRouter>
        <ErrorState error={{ response: { status: 422 } }} />
      </MemoryRouter>
    );

    expect(screen.getByText('Validation Error')).toBeInTheDocument();
  });

  it('renders 429 Too Many Requests', () => {
    render(
      <MemoryRouter>
        <ErrorState error={{ response: { status: 429 } }} />
      </MemoryRouter>
    );

    expect(screen.getByText('Too Many Requests')).toBeInTheDocument();
  });

  it('renders 500 Something Went Wrong', () => {
    render(
      <MemoryRouter>
        <ErrorState error={{ response: { status: 500 } }} />
      </MemoryRouter>
    );

    expect(screen.getByText('Something Went Wrong')).toBeInTheDocument();
  });

  it('renders 502/503 Service Temporarily Unavailable', () => {
    render(
      <MemoryRouter>
        <ErrorState error={{ response: { status: 503 } }} />
      </MemoryRouter>
    );

    expect(screen.getByText('Service Temporarily Unavailable')).toBeInTheDocument();
  });

  it('renders compact inline variant appropriately for widgets and modals', () => {
    render(
      <MemoryRouter>
        <ErrorState
          variant="inline"
          title="Widget Offline"
          message="Failed to refresh analytics chart."
        />
      </MemoryRouter>
    );

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('Widget Offline')).toBeInTheDocument();
    expect(screen.getByText('Failed to refresh analytics chart.')).toBeInTheDocument();
  });
});
