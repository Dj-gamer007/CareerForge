import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { StudentDashboardPage } from '@/features/student/StudentDashboardPage';

const server = setupServer(
  http.get('/api/v1/students/profile', () => {
    return HttpResponse.json({
      success: true,
      message: 'Profile retrieved',
      data: {
        id: 1,
        userId: 10,
        email: 'student@careerforge.local',
        firstName: 'John',
        lastName: 'Doe',
        profileCompletionPercentage: 85,
        skills: [{ id: 1, skillId: 1, skillName: 'Java', category: 'Backend', proficiency: 'ADVANCED' }],
        education: [],
        projects: [],
        certifications: [],
        resumes: [],
      },
    });
  }),
  http.get('/api/v1/students/skills', () => {
    return HttpResponse.json({
      success: true,
      message: 'Skills retrieved',
      data: [{ id: 1, skillId: 1, skillName: 'Java', category: 'Backend', proficiency: 'ADVANCED' }],
    });
  }),
  http.get('/api/v1/students/resumes', () => {
    return HttpResponse.json({
      success: true,
      message: 'Resumes retrieved',
      data: [],
    });
  }),
  http.get('/api/v1/students/applications', () => {
    return HttpResponse.json({
      success: true,
      message: 'Applications retrieved',
      data: {
        content: [
          {
            id: 100,
            jobId: 200,
            jobTitle: 'Senior Java Backend Engineer',
            jobSlug: 'senior-java-backend-engineer',
            companyId: 50,
            companyName: 'Acme Corp',
            companySlug: 'acme-corp',
            status: 'APPLIED',
            matchScoreAtApplication: 90.0,
            appliedAt: '2026-08-19T10:00:00',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 5,
        number: 0,
      },
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Student Dashboard Integration Workflow', () => {
  it('renders student profile metrics and application pipeline from API', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <StudentDashboardPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    expect(screen.getByText('Loading student workspace...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText('Welcome back, John')).toBeInTheDocument();
      expect(screen.getByText('85% Complete')).toBeInTheDocument();
      expect(screen.getByText('Senior Java Backend Engineer')).toBeInTheDocument();
      expect(screen.getByText('Acme Corp • Applied on Aug 19, 2026')).toBeInTheDocument();
    });
  });
});
