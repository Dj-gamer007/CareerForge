import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { RecruiterATSPage } from '@/features/recruiter/RecruiterATSPage';

let mockAppDetail = {
  id: 101,
  studentId: 5,
  jobId: 10,
  jobTitle: 'Backend Java Engineer',
  candidateName: 'Alex Mercer',
  candidateEmail: 'alex@careerforge.local',
  candidatePhone: '+91 9876543210',
  candidateLocation: 'Bengaluru, India',
  status: 'APPLIED',
  matchScoreAtApplication: 85.0,
  recruiterNotes: '',
  hasResume: true,
  appliedAt: '2026-08-20T10:00:00Z',
};

const server = setupServer(
  http.get('/api/v1/recruiters/jobs/10', () => {
    return HttpResponse.json({
      success: true,
      message: 'Job details',
      data: { id: 10, title: 'Backend Java Engineer', status: 'PUBLISHED' },
    });
  }),
  http.get('/api/v1/recruiters/jobs/10/applications', () => {
    return HttpResponse.json({
      success: true,
      message: 'Applications list',
      data: {
        content: [
          {
            id: 101,
            studentId: 5,
            candidateName: 'Alex Mercer',
            status: 'APPLIED',
            matchScoreAtApplication: 85.0,
            appliedAt: '2026-08-20T10:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    });
  }),
  http.get('/api/v1/recruiters/applications/101', () => {
    return HttpResponse.json({
      success: true,
      message: 'Application detail',
      data: mockAppDetail,
    });
  }),
  http.patch('/api/v1/recruiters/applications/101/notes', async ({ request }) => {
    const body = (await request.json()) as any;
    const notes = body.recruiterNotes ?? body.notes ?? '';
    mockAppDetail = {
      ...mockAppDetail,
      recruiterNotes: notes,
    };
    return HttpResponse.json({
      success: true,
      message: 'Recruiter notes updated successfully',
      data: mockAppDetail,
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  mockAppDetail = {
    id: 101,
    studentId: 5,
    jobId: 10,
    jobTitle: 'Backend Java Engineer',
    candidateName: 'Alex Mercer',
    candidateEmail: 'alex@careerforge.local',
    candidatePhone: '+91 9876543210',
    candidateLocation: 'Bengaluru, India',
    status: 'APPLIED',
    matchScoreAtApplication: 85.0,
    recruiterNotes: '',
    hasResume: true,
    appliedAt: '2026-08-20T10:00:00Z',
  };
});
afterAll(() => server.close());

describe('Recruiter Dossier & Notes Workflow', () => {
  it('opens dossier, enters internal note, saves successfully and persists on reopen', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/recruiter/jobs/10/applications']}>
          <Routes>
            <Route path="/recruiter/jobs/:jobId/applications" element={<RecruiterATSPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    // 1. Verify candidate appears in ATS
    await waitFor(() => {
      expect(screen.getByText('Alex Mercer')).toBeInTheDocument();
    });

    // 2. Open Dossier by clicking the candidate card
    const candidateCard = screen.getByText('Alex Mercer');
    fireEvent.click(candidateCard);

    // 3. Verify Dossier title & textarea rendered after detail loads
    await waitFor(() => {
      expect(screen.getByText('Candidate Dossier & Evaluation')).toBeInTheDocument();
      expect(
        screen.getByPlaceholderText(/Internal interview remarks, salary negotiations/i)
      ).toBeInTheDocument();
    });

    const textarea = screen.getByPlaceholderText(/Internal interview remarks, salary negotiations/i);

    // 4. Enter note "Not capable"
    fireEvent.change(textarea, { target: { value: 'Not capable' } });

    // 5. Click Save Internal Notes
    const saveButton = screen.getByRole('button', { name: /save internal notes/i });
    fireEvent.click(saveButton);

    // 6. Verify success feedback banner
    await waitFor(() => {
      expect(screen.getByText('Internal notes saved successfully.')).toBeInTheDocument();
    });

    // 7. Verify the textarea value is updated with the saved note
    expect(textarea).toHaveValue('Not capable');

    // 8. Close and reopen to verify persistence
    const modalCloseButtons = screen.getAllByRole('button');
    const closeBtn = modalCloseButtons.find((btn) => btn.querySelector('svg.lucide-x'));
    if (closeBtn) {
      fireEvent.click(closeBtn);
    }

    const cards = screen.getAllByText('Alex Mercer');
    fireEvent.click(cards[0]);

    await waitFor(() => {
      const reopenedTextarea = screen.getByPlaceholderText(/Internal interview remarks, salary negotiations/i);
      expect(reopenedTextarea).toHaveValue('Not capable');
    });
  });
});
