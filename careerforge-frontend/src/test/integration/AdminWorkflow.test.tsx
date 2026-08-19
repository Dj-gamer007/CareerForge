import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { AdminDashboardPage } from '@/features/admin-analytics/AdminDashboardPage';

const server = setupServer(
  http.get('/api/v1/admin/analytics/overview', () => {
    return HttpResponse.json({
      success: true,
      message: 'Overview retrieved',
      data: {
        totalUsers: 150,
        totalStudents: 100,
        totalRecruiters: 45,
        totalAdmins: 5,
        activeEnabledUsers: 145,
        disabledUsers: 5,
        totalCompanies: 30,
        verifiedCompanies: 25,
        pendingCompanies: 4,
        rejectedCompanies: 1,
        totalJobs: 50,
        publishedJobs: 35,
        draftJobs: 10,
        closedJobs: 3,
        archivedJobs: 2,
        totalApplications: 300,
        activeApplications: 250,
        acceptedApplications: 30,
        rejectedApplications: 15,
        withdrawnApplications: 5,
      },
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Admin Dashboard Integration Workflow', () => {
  it('renders aggregated overview KPI metrics from API', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminDashboardPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(screen.getByText('Loading administrative dashboard...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('150')).toBeInTheDocument(); // Total users
      expect(screen.getByText('25 Verified')).toBeInTheDocument(); // Verified companies
      expect(screen.getByText('35 Published')).toBeInTheDocument(); // Published jobs
      expect(screen.getByText('30 Placed')).toBeInTheDocument(); // Placed
    });
  });
});
