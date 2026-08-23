import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { JobDiscoveryPage } from '@/features/jobs/JobDiscoveryPage';

const mockJobs = [
  {
    id: 1,
    title: 'Java Backend Developer',
    slug: 'java-backend-developer',
    companyId: 10,
    companyName: 'CareerForge Tech',
    location: 'Chennai, India',
    workMode: 'HYBRID',
    jobType: 'FULL_TIME',
    experienceLevel: 'MID_LEVEL',
    skills: [{ id: 1, skillName: 'Java', isRequired: true }],
  },
  {
    id: 2,
    title: 'Remote React Architect',
    slug: 'remote-react-architect',
    companyId: 20,
    companyName: 'CloudScale Inc',
    location: 'Bangalore, India',
    workMode: 'REMOTE',
    jobType: 'FULL_TIME',
    experienceLevel: 'SENIOR_LEVEL',
    skills: [{ id: 2, skillName: 'React', isRequired: true }],
  },
  {
    id: 3,
    title: 'Python Data Science Intern',
    slug: 'python-data-science-intern',
    companyId: 30,
    companyName: 'AI Labs',
    location: 'Mumbai, India',
    workMode: 'ONSITE',
    jobType: 'INTERNSHIP',
    experienceLevel: 'ENTRY_LEVEL',
    skills: [{ id: 3, skillName: 'Python', isRequired: true }],
  },
];

let apiCallsCount = 0;

const server = setupServer(
  http.get('/api/v1/jobs', ({ request }) => {
    apiCallsCount++;
    const url = new URL(request.url);
    const keyword = url.searchParams.get('keyword')?.toLowerCase();
    const location = url.searchParams.get('location')?.toLowerCase();
    const workMode = url.searchParams.get('workMode');
    const jobType = url.searchParams.get('jobType');
    const experienceLevel = url.searchParams.get('experienceLevel');

    let filtered = [...mockJobs];

    if (keyword) {
      filtered = filtered.filter(
        (j) => j.title.toLowerCase().includes(keyword) || j.companyName.toLowerCase().includes(keyword)
      );
    }

    if (location) {
      if (location === 'remote') {
        filtered = filtered.filter((j) => j.workMode === 'REMOTE' || j.location.toLowerCase().includes('remote'));
      } else {
        filtered = filtered.filter((j) => j.location.toLowerCase().includes(location));
      }
    }

    if (workMode) {
      filtered = filtered.filter((j) => j.workMode === workMode);
    }

    if (jobType) {
      filtered = filtered.filter((j) => j.jobType === jobType);
    }

    if (experienceLevel) {
      filtered = filtered.filter((j) => j.experienceLevel === experienceLevel);
    }

    return HttpResponse.json({
      success: true,
      message: 'Jobs retrieved',
      data: {
        content: filtered,
        totalElements: filtered.length,
        totalPages: Math.ceil(filtered.length / 10) || 1,
        size: 10,
        number: 0,
      },
    });
  }),
  http.get('/api/v1/students/saved-jobs', () => {
    return HttpResponse.json({
      success: true,
      message: 'Saved jobs',
      data: { content: [], totalElements: 0, totalPages: 0, size: 100, number: 0 },
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  apiCallsCount = 0;
});
afterAll(() => server.close());

describe('Job Discovery Tabs & Filter Synchronization Integration', () => {
  it('renders Search Jobs and Current Openings tabs, showing search empty state initially and searching properly', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobDiscoveryPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Verify tabs
    const buttons = screen.getAllByRole('button', { name: /search jobs/i });
    expect(buttons.length).toBeGreaterThanOrEqual(1);
    expect(screen.getByRole('button', { name: /current openings/i })).toBeInTheDocument();

    // Verify initial empty state on Search Jobs tab
    expect(screen.getByText('Search for your next opportunity')).toBeInTheDocument();

    // Perform a search for React
    const searchInput = screen.getByPlaceholderText(/job title, keywords, or skills/i);
    fireEvent.change(searchInput, { target: { value: 'React' } });
    const allSearchButtons = screen.getAllByRole('button', { name: /search jobs/i });
    const submitBtn = allSearchButtons.find((b) => b.getAttribute('type') === 'submit') || allSearchButtons[1];
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Remote React Architect')).toBeInTheDocument();
    });

    // Switch to Current Openings tab
    const currentTab = screen.getByRole('button', { name: /current openings/i });
    fireEvent.click(currentTab);

    await waitFor(() => {
      expect(screen.getByText('Java Backend Developer')).toBeInTheDocument();
    });
  });

  it('validates empty search submission: does not call search API and displays validation message', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobDiscoveryPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Click Search Jobs without entering any criterion
    const allSearchButtons = screen.getAllByRole('button', { name: /search jobs/i });
    const submitBtn = allSearchButtons.find((b) => b.getAttribute('type') === 'submit') || allSearchButtons[1];
    fireEvent.click(submitBtn);

    // Check validation message
    expect(screen.getByText('Please select at least one search criterion or filter.')).toBeInTheDocument();
    expect(screen.getByText('Search for your next opportunity')).toBeInTheDocument();
    expect(apiCallsCount).toBe(0);
  });

  it('shows Reset Filters when sort is changed, and clicking Reset Filters resets sort and clears search', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobDiscoveryPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Initial state: Reset Filters button is NOT visible
    expect(screen.queryByRole('button', { name: /reset filters/i })).not.toBeInTheDocument();

    // Change Sort dropdown to 'Sort by Deadline'
    const selects = screen.getAllByRole('combobox');
    const sortSelect = selects[3]; // fourth select is Sort
    fireEvent.change(sortSelect, { target: { value: 'deadline,asc' } });

    // Reset Filters button MUST become visible
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /reset filters/i })).toBeInTheDocument();
    });

    // Click Reset Filters
    const resetBtn = screen.getByRole('button', { name: /reset filters/i });
    fireEvent.click(resetBtn);

    // Verify sort is reset, Reset Filters is hidden, and empty prompt remains
    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /reset filters/i })).not.toBeInTheDocument();
      expect(screen.getByText('Search for your next opportunity')).toBeInTheDocument();
    });
  });

  it('searches for remote jobs when entering "remote" in location search input', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobDiscoveryPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    const locationInput = screen.getByPlaceholderText(/city, state, or remote/i);
    fireEvent.change(locationInput, { target: { value: 'remote' } });
    const allSearchButtons = screen.getAllByRole('button', { name: /search jobs/i });
    const submitBtn = allSearchButtons.find((b) => b.getAttribute('type') === 'submit') || allSearchButtons[1];
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Remote React Architect')).toBeInTheDocument();
      expect(screen.queryByText('Java Backend Developer')).not.toBeInTheDocument();
    });
  });

  it('filters correctly by workMode, jobType, and experienceLevel removing non-matching jobs', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobDiscoveryPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Initial search for Java
    const searchInput = screen.getByPlaceholderText(/job title, keywords, or skills/i);
    fireEvent.change(searchInput, { target: { value: 'Java' } });
    const allSearchButtons = screen.getAllByRole('button', { name: /search jobs/i });
    const submitBtn = allSearchButtons.find((b) => b.getAttribute('type') === 'submit') || allSearchButtons[1];
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Java Backend Developer')).toBeInTheDocument();
    });

    // Change Work Mode filter to REMOTE (Java Backend Developer is HYBRID, so it must disappear)
    const selects = screen.getAllByRole('combobox');
    const workModeSelect = selects[0]; // first select is Work Mode
    fireEvent.change(workModeSelect, { target: { value: 'REMOTE' } });

    await waitFor(() => {
      expect(screen.queryByText('Java Backend Developer')).not.toBeInTheDocument();
      expect(screen.getByText('No jobs found')).toBeInTheDocument();
    });
  });

  it('clears results and returns to initial state when Reset Filters is clicked', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <JobDiscoveryPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Search for Java
    const searchInput = screen.getByPlaceholderText(/job title, keywords, or skills/i);
    fireEvent.change(searchInput, { target: { value: 'Java' } });
    const allSearchButtons = screen.getAllByRole('button', { name: /search jobs/i });
    const submitBtn = allSearchButtons.find((b) => b.getAttribute('type') === 'submit') || allSearchButtons[1];
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText('Java Backend Developer')).toBeInTheDocument();
    });

    // Click Reset Filters
    const resetBtn = screen.getByRole('button', { name: /reset filters/i });
    fireEvent.click(resetBtn);

    await waitFor(() => {
      expect(screen.getByText('Search for your next opportunity')).toBeInTheDocument();
      expect(screen.queryByText('Java Backend Developer')).not.toBeInTheDocument();
    });
  });
});
