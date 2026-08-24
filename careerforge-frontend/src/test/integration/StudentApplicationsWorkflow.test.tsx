import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { StudentApplicationsPage } from '@/features/student/StudentApplicationsPage';
const server = setupServer(
  http.get('/api/v1/students/applications/counts', () => {
    return HttpResponse.json({
      success: true,
      message: 'Counts retrieved',
      data: {
        all: 1,
        applied: 1,
        shortlisted: 0,
        interview: 0,
      },
    });
  }),
  http.get('/api/v1/students/applications', () => {
    return HttpResponse.json({
      success: true,
      message: 'Applications retrieved',
      data: {
        content: [
          {
            id: 101,
            jobId: 201,
            jobTitle: 'Full Stack Java Engineer',
            jobSlug: 'full-stack-java-engineer',
            companyId: 50,
            companyName: 'Acme Corp',
            companySlug: 'acme-corp',
            status: 'APPLIED',
            matchScoreAtApplication: 95.0,
            appliedAt: '2026-08-20T10:00:00',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    });
  }),

  http.get('/api/v1/students/applications/101/history', () => {
    return HttpResponse.json({
      success: true,
      message: 'History retrieved',
      data: [
        {
          id: 1,
          applicationId: 101,
          fromStatus: null,
          toStatus: 'APPLIED',
          changedAt: '2026-08-20T10:00:00Z',
          changedBy: 'STUDENT',
          notes: 'Application submitted by candidate',
        },
        {
          id: 2,
          applicationId: 101,
          fromStatus: 'APPLIED',
          toStatus: 'UNDER_REVIEW',
          changedAt: '2026-08-21T14:30:00Z',
          changedBy: 'RECRUITER',
          notes: 'Profile looks promising',
        },
      ],
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Student Applications Page & Timeline Modal Integration', () => {
  it('renders application card and opens timeline history modal on View History click', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <StudentApplicationsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Initial loading
    expect(screen.getByText('Loading your application tracker...')).toBeInTheDocument();

    // Verify card content and header dropdown
    await waitFor(() => {
      expect(screen.getByText('Application Pipeline & Tracker')).toBeInTheDocument();
      expect(screen.getByText('Full Stack Java Engineer')).toBeInTheDocument();
      expect(screen.getByText('Acme Corp')).toBeInTheDocument();
      expect(screen.getByText('View History')).toBeInTheDocument();
      expect(screen.getByText('Withdraw')).toBeInTheDocument();
    });

    // Verify dropdown options
    const select = screen.getByRole('combobox') as HTMLSelectElement;
    expect(select).toBeInTheDocument();
    const optionLabels = Array.from(select.options).map((opt) => opt.text);
    expect(optionLabels).toEqual([
      'All Statuses',
      'Applied',
      'Under Review',
      'Shortlisted',
      'Interview Scheduled',
      'Accepted',
      'Rejected',
      'Withdrawn',
    ]);

    // Click View History button
    fireEvent.click(screen.getByText('View History'));

    // Verify modal content
    await waitFor(() => {
      expect(screen.getByText('Application Timeline & History')).toBeInTheDocument();
      expect(screen.getByText('Initiated by Candidate')).toBeInTheDocument();
      expect(screen.getByText('Updated by Hiring Team')).toBeInTheDocument();
      expect(screen.getByText('Application submitted by candidate')).toBeInTheDocument();
      expect(screen.getByText('Profile looks promising')).toBeInTheDocument();
    });
  });
});
