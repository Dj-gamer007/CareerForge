import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { RecruiterJobsPage } from '@/features/recruiter/RecruiterJobsPage';

const mockJobs = [
  {
    id: 10,
    title: 'Full Stack Engineer',
    status: 'DRAFT',
    workMode: 'REMOTE',
    location: 'Bangalore, India',
    createdAt: '2026-08-20T10:00:00Z',
    deadline: '2026-09-30T23:59:59Z',
    skills: [{ id: 1, skillName: 'React', isRequired: true }],
  },
  {
    id: 20,
    title: 'Senior DevOps Specialist',
    status: 'PUBLISHED',
    workMode: 'HYBRID',
    location: 'Chennai, India',
    createdAt: '2026-08-15T10:00:00Z',
    deadline: '2026-09-15T23:59:59Z',
    skills: [{ id: 2, skillName: 'Docker', isRequired: true }],
  },
];

let jobsState = [...mockJobs];

const server = setupServer(
  http.get('/api/v1/recruiters/jobs', () => {
    return HttpResponse.json({
      success: true,
      message: 'Company jobs retrieved',
      data: {
        content: jobsState,
        totalElements: jobsState.length,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    });
  }),
  http.patch('/api/v1/recruiters/jobs/:id/publish', ({ params }) => {
    const jobId = Number(params.id);
    const job = jobsState.find((j) => j.id === jobId);
    if (job) {
      job.status = 'PUBLISHED';
    }
    return HttpResponse.json({
      success: true,
      message: 'Job published successfully',
      data: job,
    });
  }),
  http.patch('/api/v1/recruiters/jobs/:id/archive', ({ params }) => {
    const jobId = Number(params.id);
    const job = jobsState.find((j) => j.id === jobId);
    if (job) {
      job.status = 'ARCHIVED';
    }
    return HttpResponse.json({
      success: true,
      message: 'Job archived successfully',
      data: job,
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  jobsState = [
    {
      id: 10,
      title: 'Full Stack Engineer',
      status: 'DRAFT',
      workMode: 'REMOTE',
      location: 'Bangalore, India',
      createdAt: '2026-08-20T10:00:00Z',
      deadline: '2026-09-30T23:59:59Z',
      skills: [{ id: 1, skillName: 'React', isRequired: true }],
    },
    {
      id: 20,
      title: 'Senior DevOps Specialist',
      status: 'PUBLISHED',
      workMode: 'HYBRID',
      location: 'Chennai, India',
      createdAt: '2026-08-15T10:00:00Z',
      deadline: '2026-09-15T23:59:59Z',
      skills: [{ id: 2, skillName: 'Docker', isRequired: true }],
    },
  ];
});
afterAll(() => server.close());

describe('Recruiter Job Management Lifecycle', () => {
  it('publishes a DRAFT job and immediately updates status to PUBLISHED', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RecruiterJobsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Initial render checks
    await waitFor(() => {
      expect(screen.getByText('Full Stack Engineer')).toBeInTheDocument();
      expect(screen.getByText('DRAFT')).toBeInTheDocument();
    });

    // Find and click Publish button for the draft job
    const publishButton = screen.getByRole('button', { name: /publish/i });
    fireEvent.click(publishButton);

    // Verify feedback and status transition
    await waitFor(() => {
      expect(
        screen.getByText(/Job published successfully. It is now open for candidate applications./i)
      ).toBeInTheDocument();
    });

    await waitFor(() => {
      const badges = screen.getAllByText('PUBLISHED');
      expect(badges.length).toBe(2); // Both jobs are now published
    });
  });

  it('archives a job and updates status to ARCHIVED with success feedback', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <RecruiterJobsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Senior DevOps Specialist')).toBeInTheDocument();
    });

    const archiveButtons = screen.getAllByRole('button', { name: /archive/i });
    fireEvent.click(archiveButtons[0]);

    await waitFor(() => {
      expect(screen.getByText(/Job archived successfully/i)).toBeInTheDocument();
      expect(screen.getByText('ARCHIVED')).toBeInTheDocument();
    });
  });
});
