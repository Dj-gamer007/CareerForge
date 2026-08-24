import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { AdminDashboardPage } from '@/features/admin-analytics/AdminDashboardPage';

import { AdminUsersPage } from '@/features/admin-users/AdminUsersPage';
import { AdminCompaniesPage } from '@/features/admin-moderation/AdminCompaniesPage';
import { fireEvent } from '@testing-library/react';

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
  }),

  http.get('/api/v1/admin/users', ({ request }) => {
    const url = new URL(request.url);
    const search = url.searchParams.get('search');
    return HttpResponse.json({
      success: true,
      message: 'Users retrieved',
      data: {
        content: [
          {
            id: 10,
            email: search ? `${search}@test.com` : 'student@test.com',
            role: 'ROLE_STUDENT',
            enabled: true,
            createdAt: '2026-08-01T10:00:00',
            updatedAt: '2026-08-01T10:00:00',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    });
  }),

  http.get('/api/v1/admin/users/10', () => {
    return HttpResponse.json({
      success: true,
      message: 'User detail retrieved',
      data: {
        id: 10,
        email: 'student@test.com',
        role: 'ROLE_STUDENT',
        enabled: true,
        createdAt: '2026-08-01T10:00:00',
        updatedAt: '2026-08-01T10:00:00',
        studentProfile: {
          id: 1,
          firstName: 'Alice',
          lastName: 'Student',
          profileCompletionPercentage: 85,
          totalSkills: 4,
          totalEducations: 1,
          totalProjects: 2,
          totalCertifications: 1,
          totalResumes: 2,
          skills: ['Java', 'Spring Boot'],
        },
      },
    });
  }),

  http.get('/api/v1/admin/companies', ({ request }) => {
    const url = new URL(request.url);
    const status = url.searchParams.get('status') || url.searchParams.get('verificationStatus');
    if (status === 'PENDING') {
      return HttpResponse.json({
        success: true,
        message: 'Companies retrieved',
        data: {
          content: [],
          totalElements: 0,
          totalPages: 0,
          size: 10,
          number: 0,
        },
      });
    }
    return HttpResponse.json({
      success: true,
      message: 'Companies retrieved',
      data: {
        content: [
          {
            id: 100,
            name: 'Acme Corp',
            slug: 'acme-corp',
            verificationStatus: 'VERIFIED',
            recruiterCount: 2,
            jobCount: 5,
            createdAt: '2026-08-01T10:00:00',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
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

  it('User Directory: Inspect user shows correct non-zero resume count', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('student@test.com')).toBeInTheDocument();
    });

    // Click Inspect
    fireEvent.click(screen.getByText('Inspect'));

    // Verify modal displays real resume count (2 uploaded)
    await waitFor(() => {
      expect(screen.getByText('User Account Details')).toBeInTheDocument();
      expect(screen.getByText('2 uploaded')).toBeInTheDocument();
      expect(screen.getByText('4 verified skills')).toBeInTheDocument();
    });
  });

  it('Company Verification: correctly applies verification filter and shows empty state when 0 matches', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminCompaniesPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    });

    // Select Pending Verification
    const select = screen.getByRole('combobox') as HTMLSelectElement;
    fireEvent.change(select, { target: { value: 'PENDING' } });

    await waitFor(() => {
      expect(screen.getByText('No companies in queue')).toBeInTheDocument();
      expect(screen.queryByText('Acme Corp')).not.toBeInTheDocument();
    });
  });
});
