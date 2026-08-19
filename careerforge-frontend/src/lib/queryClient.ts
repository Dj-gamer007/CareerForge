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
    applications: (filters?: Record<string, unknown>) => ['student', 'applications', filters] as const,
    applicationDetail: (id: number) => ['student', 'applications', id] as const,
    matchPreview: (jobId: number) => ['student', 'match-preview', jobId] as const,
    savedJobs: (page?: number) => ['student', 'saved-jobs', page] as const,
    isSaved: (jobId: number) => ['student', 'saved-jobs', 'check', jobId] as const,
  },
  recruiter: {
    profile: ['recruiter', 'profile'] as const,
    company: ['recruiter', 'company'] as const,
    jobs: (filters?: Record<string, unknown>) => ['recruiter', 'jobs', filters] as const,
    jobDetail: (id: number) => ['recruiter', 'jobs', id] as const,
    applications: (jobId: number, filters?: Record<string, unknown>) =>
      ['recruiter', 'applications', jobId, filters] as const,
    applicationDetail: (id: number) => ['recruiter', 'applications', 'detail', id] as const,
  },
  publicJobs: {
    list: (filters?: Record<string, unknown>) => ['public', 'jobs', filters] as const,
    detail: (slugOrId: string | number) => ['public', 'jobs', 'detail', slugOrId] as const,
  },
  publicCompanies: {
    list: (filters?: Record<string, unknown>) => ['public', 'companies', filters] as const,
    detail: (slugOrId: string | number) => ['public', 'companies', 'detail', slugOrId] as const,
  },
  notifications: {
    list: (page?: number) => ['notifications', 'list', page] as const,
    unreadCount: ['notifications', 'unread-count'] as const,
  },
  admin: {
    users: (filters?: Record<string, unknown>) => ['admin', 'users', filters] as const,
    userDetail: (id: number) => ['admin', 'users', id] as const,
    companies: (filters?: Record<string, unknown>) => ['admin', 'companies', filters] as const,
    companyDetail: (id: number) => ['admin', 'companies', id] as const,
    jobs: (filters?: Record<string, unknown>) => ['admin', 'jobs', filters] as const,
    jobDetail: (id: number) => ['admin', 'jobs', id] as const,
    auditLogs: (filters?: Record<string, unknown>) => ['admin', 'audit-logs', filters] as const,
    auditLogDetail: (id: number) => ['admin', 'audit-logs', id] as const,
    analyticsOverview: ['admin', 'analytics', 'overview'] as const,
    analyticsFunnel: (params?: Record<string, unknown>) => ['admin', 'analytics', 'funnel', params] as const,
    analyticsJobs: (params?: Record<string, unknown>) => ['admin', 'analytics', 'jobs', params] as const,
    analyticsCompanies: ['admin', 'analytics', 'companies'] as const,
    analyticsUsers: ['admin', 'analytics', 'users'] as const,
    analyticsTrends: (days: number) => ['admin', 'analytics', 'trends', days] as const,
  },
};
