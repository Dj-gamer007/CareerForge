import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { StudentSavedJobsPage } from '@/features/student/StudentSavedJobsPage';

let mockSavedJobs: any[] = [];

const server = setupServer(
  http.get('/api/v1/students/saved-jobs', () => {
    return HttpResponse.json({
      success: true,
      message: 'Saved jobs retrieved',
      data: {
        content: mockSavedJobs,
        totalElements: mockSavedJobs.length,
        totalPages: mockSavedJobs.length > 0 ? 1 : 0,
        size: 10,
        number: 0,
      },
    });
  }),
  http.delete('/api/v1/students/saved-jobs/:jobId', () => {
    mockSavedJobs = [];
    return HttpResponse.json({
      success: true,
      message: 'Job removed from saved list',
      data: null,
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => server.resetHandlers());
afterAll(() => server.close());

describe('Saved Jobs Workflow Integration', () => {
  it('renders empty state and provides Explore Jobs navigation CTA when no bookmarks exist', async () => {
    mockSavedJobs = [];
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/student/saved-jobs']}>
          <Routes>
            <Route path="/student/saved-jobs" element={<StudentSavedJobsPage />} />
            <Route path="/jobs" element={<div>Find Jobs Page Route Target</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('No bookmarks saved')).toBeInTheDocument();
    });

    const exploreButton = screen.getByRole('button', { name: /explore jobs/i });
    expect(exploreButton).toBeInTheDocument();
    fireEvent.click(exploreButton);

    await waitFor(() => {
      expect(screen.getByText('Find Jobs Page Route Target')).toBeInTheDocument();
    });
  });

  it('renders saved jobs list and handles remove mutation', async () => {
    mockSavedJobs = [
      {
        id: 1,
        jobId: 10,
        jobTitle: 'Java Backend Developer',
        jobSlug: 'java-backend-developer',
        companyId: 5,
        companyName: 'CareerForge Technologies',
        location: 'Chennai, India',
        workMode: 'HYBRID',
        jobType: 'FULL_TIME',
        experienceLevel: 'MID_LEVEL',
        status: 'PUBLISHED',
        savedAt: '2026-08-21T10:00:00',
      },
    ];

    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/student/saved-jobs']}>
          <Routes>
            <Route path="/student/saved-jobs" element={<StudentSavedJobsPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Java Backend Developer')).toBeInTheDocument();
      expect(screen.getByText(/CareerForge Technologies/i)).toBeInTheDocument();
    });

    const removeBtn = screen.getByRole('button', { name: /remove/i });
    fireEvent.click(removeBtn);

    await waitFor(() => {
      expect(screen.getByText('No bookmarks saved')).toBeInTheDocument();
    });
  });
});
