import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { JobDetailPage } from '@/features/jobs/JobDetailPage';
import { useAuthStore } from '@/features/auth/authStore';

const mockMixedJob = {
  id: 100,
  title: 'Full Stack Software Engineer',
  slug: 'full-stack-software-engineer',
  description: 'Join our high performance engineering team.',
  companyId: 50,
  companyName: 'Acme Technologies',
  companySlug: 'acme-technologies',
  location: 'Bangalore, India',
  workMode: 'HYBRID',
  jobType: 'FULL_TIME',
  experienceLevel: 'MID_LEVEL',
  currency: 'INR',
  salaryMin: 1200000,
  salaryMax: 1800000,
  status: 'PUBLISHED',
  deadline: '2026-12-31',
  createdAt: '2026-08-20T10:00:00Z',
  updatedAt: '2026-08-20T10:00:00Z',
  skills: [
    {
      id: 1,
      skillId: 10,
      skillName: 'Java',
      category: 'Backend',
      required: true,
      minimumProficiency: 'INTERMEDIATE',
    },
    {
      id: 2,
      skillId: 11,
      skillName: 'Python',
      category: 'Backend',
      required: true,
      minimumProficiency: 'INTERMEDIATE',
    },
    {
      id: 3,
      skillId: 12,
      skillName: 'MySQL',
      category: 'Database',
      required: false,
      minimumProficiency: 'INTERMEDIATE',
    },
    {
      id: 4,
      skillId: 13,
      skillName: 'Docker',
      category: 'DevOps',
      required: false,
      minimumProficiency: 'BEGINNER',
    },
    {
      id: 5,
      skillId: 14,
      skillName: 'Spring Boot',
      category: 'Backend',
      required: true,
      minimumProficiency: 'ADVANCED',
    },
  ],
};

const server = setupServer(
  http.get('/api/v1/jobs/slug/full-stack-software-engineer', () => {
    return HttpResponse.json({
      success: true,
      data: mockMixedJob,
    });
  }),
  http.get('/api/v1/jobs/100', () => {
    return HttpResponse.json({
      success: true,
      data: mockMixedJob,
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  useAuthStore.getState().logout();
});
afterAll(() => server.close());

describe('JobDetailPage — Target Technical Competencies Integration', () => {
  it('correctly displays REQUIRED and OPTIONAL badges and minimum proficiencies for mixed skills', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/jobs/full-stack-software-engineer']}>
          <Routes>
            <Route path="/jobs/:slug" element={<JobDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Target Technical Competencies')).toBeInTheDocument();
      expect(screen.getByText('Full Stack Software Engineer')).toBeInTheDocument();
    });

    // Check Skill Names
    expect(screen.getByText('Java')).toBeInTheDocument();
    expect(screen.getByText('Python')).toBeInTheDocument();
    expect(screen.getByText('MySQL')).toBeInTheDocument();
    expect(screen.getByText('Docker')).toBeInTheDocument();
    expect(screen.getByText('Spring Boot')).toBeInTheDocument();

    // Check REQUIRED / OPTIONAL counts
    const requiredBadges = screen.getAllByText('REQUIRED');
    const optionalBadges = screen.getAllByText('OPTIONAL');

    expect(requiredBadges).toHaveLength(3); // Java, Python, Spring Boot
    expect(optionalBadges).toHaveLength(2); // MySQL, Docker

    // Check Minimum Proficiency text
    expect(screen.getByText('ADVANCED')).toBeInTheDocument();
    expect(screen.getByText('BEGINNER')).toBeInTheDocument();
  });

  it('renders Application Deleted state when a student previously applied but the application was deleted', async () => {
    useAuthStore.getState().setAuth('mock-access-token', 'mock-refresh-token', {
      id: 26,
      email: 'student@example.com',
      role: 'ROLE_STUDENT',
    });

    server.use(
      http.get('/api/v1/jobs/slug/sale-force-developer-f13225', () => {
        return new HttpResponse(null, { status: 404 });
      }),
      http.get('/api/v1/students/applications', () => {
        return HttpResponse.json({
          success: true,
          data: { content: [] },
        });
      }),
      http.get('/api/v1/notifications', () => {
        return HttpResponse.json({
          success: true,
          data: {
            content: [
              {
                id: 99,
                title: 'Application Submitted: Sale force Developer worker',
                message:
                  "Your application for 'Sale force Developer worker' at delite works has been successfully submitted.",
                type: 'APPLICATION_SUBMITTED',
                read: true,
                createdAt: '2026-08-20T10:00:00Z',
              },
            ],
          },
        });
      })
    );

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/jobs/sale-force-developer-f13225']}>
          <Routes>
            <Route path="/jobs/:slug" element={<JobDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Job Position Deleted')).toBeInTheDocument();
    });

    expect(
      screen.getByText(/The 'Sale force Developer worker' position at 'delite works' has been deleted due to business requirements/i)
    ).toBeInTheDocument();

    const searchBtn = screen.getByRole('button', { name: /search for another job/i });
    expect(searchBtn).toBeInTheDocument();
  });

  it('renders generic Resource Not Found for a genuinely non-existent job when user never applied', async () => {
    server.use(
      http.get('/api/v1/jobs/slug/completely-nonexistent-job-xyz', () => {
        return new HttpResponse(null, { status: 404 });
      }),
      http.get('/api/v1/students/applications', () => {
        return HttpResponse.json({
          success: true,
          data: { content: [] },
        });
      }),
      http.get('/api/v1/notifications', () => {
        return HttpResponse.json({
          success: true,
          data: { content: [] },
        });
      })
    );

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/jobs/completely-nonexistent-job-xyz']}>
          <Routes>
            <Route path="/jobs/:slug" element={<JobDetailPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Resource Not Found')).toBeInTheDocument();
    });
  });
});
