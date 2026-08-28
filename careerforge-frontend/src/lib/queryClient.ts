import { QueryClient } from '@tanstack/react-query';

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 1000 * 60 * 2, // 2 minutes
      retry: (failureCount, error: any) => {
        if (error?.response?.status === 401 || error?.response?.status === 403 || error?.response?.status === 404) {
          return false;
        }
        return failureCount < 2;
      },
      refetchOnWindowFocus: false,
    },
  },
});

export const queryKeys = {
  auth: {
    me: ['auth', 'me'] as const,
  },
  student: {
    profile: ['student', 'profile'] as const,
    skills: ['student', 'skills'] as const,
    resumes: ['student', 'resumes'] as const,
    applications: (filters?: Record<string, unknown>) =>
      filters ? (['student', 'applications', filters] as const) : (['student', 'applications'] as const),
    applicationDetail: (id: number) => ['student', 'applications', id] as const,
    matchPreview: (jobId: number) => ['student', 'match-preview', jobId] as const,
    savedJobs: (page?: number) =>
      page !== undefined ? (['student', 'saved-jobs', page] as const) : (['student', 'saved-jobs'] as const),
    isSaved: (jobId: number) => ['student', 'saved-jobs', 'check', jobId] as const,
  },
  recruiter: {
    profile: ['recruiter', 'profile'] as const,
    company: (userId?: number) =>
      userId !== undefined ? (['recruiter', 'company', userId] as const) : (['recruiter', 'company'] as const),
    jobs: (filters?: Record<string, unknown>) =>
      filters ? (['recruiter', 'jobs', filters] as const) : (['recruiter', 'jobs'] as const),
    jobDetail: (id: number) => ['recruiter', 'jobs', id] as const,
    applications: (jobId: number, filters?: Record<string, unknown>) =>
      filters
        ? (['recruiter', 'applications', jobId, filters] as const)
        : (['recruiter', 'applications', jobId] as const),
    applicationDetail: (id: number) => ['recruiter', 'applications', 'detail', id] as const,
  },
  publicJobs: {
    list: (filters?: Record<string, unknown>) =>
      filters ? (['public', 'jobs', filters] as const) : (['public', 'jobs'] as const),
    detail: (slugOrId: string | number) => ['public', 'jobs', 'detail', slugOrId] as const,
  },
  publicCompanies: {
    list: (filters?: Record<string, unknown>) =>
      filters ? (['public', 'companies', filters] as const) : (['public', 'companies'] as const),
    detail: (slugOrId: string | number) => ['public', 'companies', 'detail', slugOrId] as const,
  },
  notifications: {
    list: (page?: number) =>
      page !== undefined ? (['notifications', 'list', page] as const) : (['notifications', 'list'] as const),
    unreadCount: ['notifications', 'unread-count'] as const,
  },
  admin: {
    users: (filters?: Record<string, unknown>) =>
      filters ? (['admin', 'users', filters] as const) : (['admin', 'users'] as const),
    userDetail: (id: number) => ['admin', 'users', id] as const,
    companies: (filters?: Record<string, unknown>) =>
      filters ? (['admin', 'companies', filters] as const) : (['admin', 'companies'] as const),
    companyDetail: (id: number) => ['admin', 'companies', id] as const,
    jobs: (filters?: Record<string, unknown>) =>
      filters ? (['admin', 'jobs', filters] as const) : (['admin', 'jobs'] as const),
    jobDetail: (id: number) => ['admin', 'jobs', id] as const,
    auditLogs: (filters?: Record<string, unknown>) =>
      filters ? (['admin', 'audit-logs', filters] as const) : (['admin', 'audit-logs'] as const),
    auditLogDetail: (id: number) => ['admin', 'audit-logs', id] as const,
    analyticsOverview: ['admin', 'analytics', 'overview'] as const,
    analyticsFunnel: (params?: Record<string, unknown>) =>
      params ? (['admin', 'analytics', 'funnel', params] as const) : (['admin', 'analytics', 'funnel'] as const),
    analyticsJobs: (params?: Record<string, unknown>) =>
      params ? (['admin', 'analytics', 'jobs', params] as const) : (['admin', 'analytics', 'jobs'] as const),
    analyticsCompanies: ['admin', 'analytics', 'companies'] as const,
    analyticsUsers: ['admin', 'analytics', 'users'] as const,
    analyticsTrends: (days?: number) =>
      days !== undefined ? (['admin', 'analytics', 'trends', days] as const) : (['admin', 'analytics', 'trends'] as const),
  },
};
