import { describe, it, expect, beforeAll, afterEach, afterAll } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { setupServer } from 'msw/node';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { NotificationBell } from '@/features/notifications/NotificationBell';
import { NotificationListPage } from '@/features/notifications/NotificationListPage';
import { StudentApplicationsPage } from '@/features/student/StudentApplicationsPage';
import { RecruiterATSPage } from '@/features/recruiter/RecruiterATSPage';
import { AdminUsersPage } from '@/features/admin-users/AdminUsersPage';
import { useAuthStore } from '@/features/auth/authStore';
import { NotificationResponse } from '@/types/notification.types';

let unreadCountState = 1;
let notificationsState: Array<NotificationResponse & { updatedAt?: string }> = [
  {
    id: 101,
    title: 'New Job Posted',
    message: 'A new job opportunity has been posted: Java Backend Developer at ABC Technologies.',
    type: 'JOB_RECOMMENDATION',
    read: false,
    createdAt: '2026-08-25T10:00:00Z',
    updatedAt: '2026-08-25T10:00:00Z',
  },
];

let studentAppsState = [
  {
    id: 1,
    jobId: 10,
    jobTitle: 'Cloud Backend Engineer',
    jobSlug: 'cloud-backend-engineer',
    companyId: 5,
    companyName: 'ABC Technologies',
    companySlug: 'abc-technologies',
    status: 'APPLIED',
    appliedAt: '2026-08-25T09:00:00Z',
    matchScoreAtApplication: 95.0,
  },
];

let recruiterAppsState = [
  {
    id: 1,
    studentId: 100,
    candidateName: 'Alice Student',
    candidateEmail: 'alice@careerforge.local',
    status: 'APPLIED',
    appliedAt: '2026-08-25T09:00:00Z',
    overallScore: 92.5,
  },
];

let usersList = [
  {
    id: 1,
    email: 'admin@careerforge.local',
    role: 'ROLE_ADMIN',
    enabled: true,
    createdAt: '2026-08-01T00:00:00Z',
    linkedProfileName: 'System Administrator',
  },
  {
    id: 2,
    email: 'student@careerforge.local',
    role: 'ROLE_STUDENT',
    enabled: true,
    createdAt: '2026-08-01T00:00:00Z',
    linkedProfileName: 'Alice Student',
  },
];

const server = setupServer(
  http.get('/api/v1/notifications/unread-count', () => {
    return HttpResponse.json({
      success: true,
      data: { unreadCount: unreadCountState },
    });
  }),
  http.get('/api/v1/notifications', () => {
    return HttpResponse.json({
      success: true,
      data: {
        content: notificationsState,
        totalElements: notificationsState.length,
        totalPages: 1,
        size: 15,
        number: 0,
      },
    });
  }),
  http.patch('/api/v1/notifications/:id/read', ({ params }) => {
    const id = Number(params.id);
    notificationsState = notificationsState.map((n) => (n.id === id ? { ...n, read: true } : n));
    unreadCountState = Math.max(0, unreadCountState - 1);
    return HttpResponse.json({
      success: true,
      data: notificationsState.find((n) => n.id === id),
    });
  }),
  http.patch('/api/v1/notifications/read-all', () => {
    notificationsState = notificationsState.map((n) => ({ ...n, read: true }));
    unreadCountState = 0;
    return HttpResponse.json({
      success: true,
      message: 'All marked as read',
    });
  }),
  http.get('/api/v1/students/applications', () => {
    return HttpResponse.json({
      success: true,
      data: {
        content: studentAppsState,
        totalElements: studentAppsState.length,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    });
  }),
  http.get('/api/v1/recruiters/jobs/:jobId/applications', () => {
    return HttpResponse.json({
      success: true,
      data: {
        content: recruiterAppsState,
        totalElements: recruiterAppsState.length,
        totalPages: 1,
        size: 100,
        number: 0,
      },
    });
  }),
  http.get('/api/v1/recruiters/jobs/:jobId', () => {
    return HttpResponse.json({
      success: true,
      data: {
        id: 10,
        title: 'Cloud Backend Engineer',
        status: 'PUBLISHED',
        skills: [{ id: 1, skillName: 'Java', isRequired: true }],
      },
    });
  }),
  http.get('/api/v1/admin/users', () => {
    return HttpResponse.json({
      success: true,
      data: {
        content: usersList,
        totalElements: usersList.length,
        totalPages: 1,
        size: 10,
        number: 0,
      },
    });
  })
);

beforeAll(() => server.listen());
afterEach(() => {
  server.resetHandlers();
  unreadCountState = 1;
  notificationsState = [
    {
      id: 101,
      title: 'New Job Posted',
      message: 'A new job opportunity has been posted: Java Backend Developer at ABC Technologies.',
      type: 'JOB_RECOMMENDATION',
      read: false,
      createdAt: '2026-08-25T10:00:00Z',
      updatedAt: '2026-08-25T10:00:00Z',
    },
  ];
});
afterAll(() => server.close());

function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false },
    },
  });
}

describe('Job Publication Notifications & Real-Time Auto-Refresh', () => {
  it('Student Notification Center displays New Job Posted notification with correct title, company, and unread count', async () => {
    const queryClient = createTestQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <NotificationBell />
        </MemoryRouter>
      </QueryClientProvider>
    );

    // 1. Badge shows unread count
    await waitFor(() => {
      expect(screen.getByText('1')).toBeInTheDocument();
    });

    // 2. Open Notification dropdown
    const bellBtn = screen.getByLabelText('Notifications');
    fireEvent.click(bellBtn);

    await waitFor(() => {
      expect(screen.getByText('New Job Posted')).toBeInTheDocument();
      expect(screen.getByText(/Java Backend Developer at ABC Technologies/)).toBeInTheDocument();
    });
  });

  it('NotificationListPage marks notification as read', async () => {
    const queryClient = createTestQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <NotificationListPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('New Job Posted')).toBeInTheDocument();
    });

    const markAllBtn = screen.getByText('Mark All as Read');
    fireEvent.click(markAllBtn);

    await waitFor(() => {
      expect(unreadCountState).toBe(0);
    });
  });

  it('Student Applications page automatically renders application status', async () => {
    const queryClient = createTestQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <StudentApplicationsPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Cloud Backend Engineer')).toBeInTheDocument();
      expect(screen.getByText('ABC Technologies')).toBeInTheDocument();
      expect(screen.getByText('Applied')).toBeInTheDocument();
    });
  });

  it('Recruiter ATS page automatically loads candidate applications without crashing', async () => {
    const queryClient = createTestQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/recruiter/jobs/10/ats']}>
          <Routes>
            <Route path="/recruiter/jobs/:jobId/ats" element={<RecruiterATSPage />} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('Alice Student')).toBeInTheDocument();
    });
  });

  it('Admin Users page retains search input focus during typing and background updates', async () => {
    useAuthStore.setState({
      user: { id: 1, email: 'admin@careerforge.local', role: 'ROLE_ADMIN' },
      isAuthenticated: true,
    });

    const queryClient = createTestQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminUsersPage />
        </MemoryRouter>
      </QueryClientProvider>
    );

    await waitFor(() => {
      expect(screen.getByText('admin@careerforge.local')).toBeInTheDocument();
    });

    const searchInput = screen.getByPlaceholderText('Search by email, name...');
    searchInput.focus();
    expect(document.activeElement).toBe(searchInput);

    fireEvent.change(searchInput, { target: { value: 'alice' } });
    expect(searchInput).toHaveValue('alice');
    expect(document.activeElement).toBe(searchInput);
  });

  it('Admin Notification Bell displays New Company Pending Verification and clicks navigate to Admin Companies', async () => {
    notificationsState = [
      {
        id: 201,
        title: 'New Company Pending Verification',
        message: 'Fintech Innovations has registered and is waiting for verification.',
        type: 'SYSTEM_ALERT',
        read: false,
        createdAt: '2026-08-25T11:00:00Z',
        updatedAt: '2026-08-25T11:00:00Z',
      },
    ];
    unreadCountState = 1;

    const queryClient = createTestQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/admin/dashboard']}>
          <Routes>
            <Route path="/admin/dashboard" element={<NotificationBell />} />
            <Route path="/admin/companies" element={<div>Admin Company Verification View</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    // 1. Open bell
    const bellBtn = screen.getByLabelText('Notifications');
    fireEvent.click(bellBtn);

    await waitFor(() => {
      expect(screen.getByText('New Company Pending Verification')).toBeInTheDocument();
      expect(screen.getByText(/Fintech Innovations has registered/)).toBeInTheDocument();
    });

    // 2. Click notification item -> navigates to /admin/companies
    const notifItem = screen.getByText('New Company Pending Verification');
    fireEvent.click(notifItem);

    await waitFor(() => {
      expect(screen.getByText('Admin Company Verification View')).toBeInTheDocument();
    });
  });

  it('Admin Notification Bell displays New Job Pending Moderation and clicks navigate to Admin Jobs', async () => {
    notificationsState = [
      {
        id: 202,
        title: 'New Job Pending Moderation',
        message: 'Lead Architect at TechCorp Solutions requires review.',
        type: 'SYSTEM_ALERT',
        read: false,
        createdAt: '2026-08-25T11:30:00Z',
        updatedAt: '2026-08-25T11:30:00Z',
      },
    ];
    unreadCountState = 1;

    const queryClient = createTestQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/admin/dashboard']}>
          <Routes>
            <Route path="/admin/dashboard" element={<NotificationBell />} />
            <Route path="/admin/jobs" element={<div>Admin Job Moderation View</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    // 1. Open bell
    const bellBtn = screen.getByLabelText('Notifications');
    fireEvent.click(bellBtn);

    await waitFor(() => {
      expect(screen.getByText('New Job Pending Moderation')).toBeInTheDocument();
      expect(screen.getByText(/Lead Architect at TechCorp Solutions/)).toBeInTheDocument();
    });

    // 2. Click notification item -> navigates to /admin/jobs
    const notifItem = screen.getByText('New Job Pending Moderation');
    fireEvent.click(notifItem);

    await waitFor(() => {
      expect(screen.getByText('Admin Job Moderation View')).toBeInTheDocument();
    });
  });

  it('renders role-based notifications with actor information and navigates based on role', async () => {
    useAuthStore.getState().setUser({
      id: 5,
      email: 'student@careerforge.local',
      role: 'ROLE_STUDENT',
    });

    notificationsState = [
      {
        id: 301,
        title: 'Application Shortlisted',
        message: "Your application for 'Java Developer' at Delite Works has been shortlisted by the hiring team.",
        type: 'APPLICATION_SHORTLISTED',
        read: false,
        actorName: 'John Smith',
        actorUserId: 2,
        relatedEntityType: 'APPLICATION',
        relatedEntityId: 50,
        createdAt: '2026-08-26T10:00:00Z',
        updatedAt: '2026-08-26T10:00:00Z',
      },
      {
        id: 302,
        title: 'Company Verified',
        message: "Your company 'Delite Works' has been verified by the CareerForge Admin team.",
        type: 'COMPANY_VERIFIED',
        read: false,
        actorName: 'CareerForge Admin',
        actorUserId: 1,
        relatedEntityType: 'COMPANY',
        relatedEntityId: 10,
        createdAt: '2026-08-26T11:00:00Z',
        updatedAt: '2026-08-26T11:00:00Z',
      },
    ];
    unreadCountState = 2;

    const queryClient = createTestQueryClient();
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/notifications']}>
          <Routes>
            <Route path="/notifications" element={<NotificationListPage />} />
            <Route path="/student/applications" element={<div>Student Applications View</div>} />
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>
    );

    // Verify actor name rendered
    await waitFor(() => {
      expect(screen.getByText('Application Shortlisted')).toBeInTheDocument();
      expect(screen.getByText('Updated by John Smith')).toBeInTheDocument();
      expect(screen.getByText('Company Verified')).toBeInTheDocument();
      expect(screen.getByText('Updated by CareerForge Admin')).toBeInTheDocument();
    });

    // Click shortlisted notification -> navigates to student applications
    const shortlistCard = screen.getByText('Application Shortlisted');
    fireEvent.click(shortlistCard);

    await waitFor(() => {
      expect(screen.getByText('Student Applications View')).toBeInTheDocument();
    });
  });
});
