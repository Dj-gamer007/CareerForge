import { describe, it, expect, beforeAll, beforeEach, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { useAuthStore } from '@/features/auth/authStore';
import { RecruiterDashboardPage } from '@/features/recruiter/RecruiterDashboardPage';
import { RecruiterCompanyPage } from '@/features/recruiter/RecruiterCompanyPage';

let companyData: any = null;

const server = setupServer(
  http.get('/api/v1/companies/my-company', () => {
    if (!companyData) {
      return HttpResponse.json({
        success: true,
        message: 'Recruiter has no company registered yet.',
        data: null,
      });
    }
    return HttpResponse.json({
      success: true,
      message: 'Company retrieved',
      data: companyData,
    });
  }),
  http.get('/api/v1/recruiters/jobs', () => {
    return HttpResponse.json({
      success: true,
      message: 'Jobs retrieved',
      data: {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size: 10,
        page: 0,
      },
    });
  })
);

beforeAll(() => server.listen({ onUnhandledRequest: 'bypass' }));
beforeEach(() => {
  useAuthStore.setState({
    isAuthenticated: true,
    isLoading: false,
    isInitialized: true,
    user: { id: 19, email: 'recruiter@careerforge.local', role: 'ROLE_RECRUITER', fullName: 'Test Recruiter' } as any,
    accessToken: 'mock-recruiter-token',
  });
});
afterEach(() => {
  server.resetHandlers();
  companyData = null;
});
afterAll(() => server.close());

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        retry: false,
      },
    },
  });
}

describe('Recruiter Company & Dashboard Workflow', () => {
  it('renders onboarding state when recruiter has no company (200 data:null) without getting stuck on loading', async () => {
    companyData = null;
    const queryClient = createTestQueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RecruiterDashboardPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Should cleanly show onboarding card
    await waitFor(() => {
      expect(screen.getByText('Welcome to CareerForge Employer Portal')).toBeInTheDocument();
      expect(screen.getByText('Register Company Profile')).toBeInTheDocument();
    });

    // Should not show loading workspace spinner
    expect(screen.queryByText('Loading recruiter workspace...')).not.toBeInTheDocument();
  });

  it('renders company workspace when recruiter is associated with a company', async () => {
    companyData = {
      id: 10,
      name: 'Acme Systems',
      slug: 'acme-systems',
      industry: 'Software',
      verificationStatus: 'PENDING',
    };
    const queryClient = createTestQueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RecruiterDashboardPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Acme Systems')).toBeInTheDocument();
      expect(screen.getByText('Organization Status: PENDING')).toBeInTheDocument();
    });
  });

  it('displays ErrorState on real 500 server error and allows retry', async () => {
    server.use(
      http.get('/api/v1/companies/my-company', () => {
        return HttpResponse.json(
          {
            status: 500,
            error: 'Internal Server Error',
            message: 'Database connection failed',
            path: '/api/v1/companies/my-company',
          },
          { status: 500 }
        );
      })
    );

    const queryClient = createTestQueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RecruiterDashboardPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Could not load recruiter workspace')).toBeInTheDocument();
      expect(screen.getByText(/Database connection failed/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /try again/i })).toBeInTheDocument();
    });
  });

  it('RecruiterCompanyPage renders empty state with register button when no company exists', async () => {
    companyData = null;
    const queryClient = createTestQueryClient();

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RecruiterCompanyPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('You have not registered an employer organization yet.')).toBeInTheDocument();
    });
  });
});
