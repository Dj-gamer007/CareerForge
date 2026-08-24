import { apiClient } from '@/lib/axios';
import { ApiResponse, SpringPage } from '@/types/api.types';
import {
  AdminUserSummaryResponse,
  AdminUserDetailResponse,
  AdminCompanySummaryResponse,
  AdminCompanyDetailResponse,
  AdminJobSummaryResponse,
  AdminJobDetailResponse,
} from '@/types/admin.types';
import { AuditLogSummaryResponse, AuditLogDetailResponse, AuditEventType, AuditTargetType, AuditStatus } from '@/types/audit.types';
import {
  PlatformOverviewResponse,
  ApplicationFunnelResponse,
  JobAnalyticsResponse,
  CompanyAnalyticsResponse,
  UserAnalyticsResponse,
  PlatformTrendsResponse,
} from '@/types/analytics.types';
import { Role } from '@/types/auth.types';
import { CompanyVerificationStatus } from '@/types/company.types';
import { JobStatus, WorkMode } from '@/types/job.types';

export const adminService = {
  // Users
  async getUsers(params?: {
    search?: string;
    role?: Role;
    enabled?: boolean;
    registeredAfter?: string;
    registeredBefore?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Promise<SpringPage<AdminUserSummaryResponse>> {
    const res = await apiClient.get<ApiResponse<SpringPage<AdminUserSummaryResponse>>>('/admin/users', { params });
    return res.data.data;
  },

  async getUserById(id: number): Promise<AdminUserDetailResponse> {
    const res = await apiClient.get<ApiResponse<AdminUserDetailResponse>>(`/admin/users/${id}`);
    return res.data.data;
  },

  async updateUserStatus(id: number, payload: { enabled: boolean; reason: string }): Promise<AdminUserSummaryResponse> {
    const res = await apiClient.patch<ApiResponse<AdminUserSummaryResponse>>(`/admin/users/${id}/status`, payload);
    return res.data.data;
  },

  // Companies
  async getCompanies(params?: {
    search?: string;
    status?: CompanyVerificationStatus;
    verificationStatus?: CompanyVerificationStatus;
    page?: number;
    size?: number;
    sort?: string;
  }): Promise<SpringPage<AdminCompanySummaryResponse>> {
    const queryParams = params
      ? {
          ...params,
          verificationStatus: params.verificationStatus || params.status,
          status: params.status || params.verificationStatus,
        }
      : undefined;
    const res = await apiClient.get<ApiResponse<SpringPage<AdminCompanySummaryResponse>>>('/admin/companies', { params: queryParams });
    return res.data.data;
  },

  async getCompanyById(id: number): Promise<AdminCompanyDetailResponse> {
    const res = await apiClient.get<ApiResponse<AdminCompanyDetailResponse>>(`/admin/companies/${id}`);
    return res.data.data;
  },

  async verifyCompany(
    id: number,
    payload: { verificationStatus: CompanyVerificationStatus; reason: string }
  ): Promise<AdminCompanyDetailResponse> {
    const res = await apiClient.patch<ApiResponse<AdminCompanyDetailResponse>>(`/admin/companies/${id}/verification`, payload);
    return res.data.data;
  },

  // Jobs
  async getJobs(params?: {
    search?: string;
    status?: JobStatus;
    companyId?: number;
    workMode?: WorkMode;
    page?: number;
    size?: number;
    sort?: string;
  }): Promise<SpringPage<AdminJobSummaryResponse>> {
    const res = await apiClient.get<ApiResponse<SpringPage<AdminJobSummaryResponse>>>('/admin/jobs', { params });
    return res.data.data;
  },

  async getJobById(id: number): Promise<AdminJobDetailResponse> {
    const res = await apiClient.get<ApiResponse<AdminJobDetailResponse>>(`/admin/jobs/${id}`);
    return res.data.data;
  },

  async moderateJob(id: number, payload: { status: JobStatus; reason: string }): Promise<AdminJobDetailResponse> {
    const res = await apiClient.patch<ApiResponse<AdminJobDetailResponse>>(`/admin/jobs/${id}/moderate`, payload);
    return res.data.data;
  },

  // Audit Logs
  async getAuditLogs(params?: {
    search?: string;
    eventType?: AuditEventType;
    targetType?: AuditTargetType;
    status?: AuditStatus;
    actorUserId?: number;
    dateFrom?: string;
    dateTo?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Promise<SpringPage<AuditLogSummaryResponse>> {
    const res = await apiClient.get<ApiResponse<SpringPage<AuditLogSummaryResponse>>>('/admin/audit-logs', { params });
    return res.data.data;
  },

  async getAuditLogById(id: number): Promise<AuditLogDetailResponse> {
    const res = await apiClient.get<ApiResponse<AuditLogDetailResponse>>(`/admin/audit-logs/${id}`);
    return res.data.data;
  },

  // Analytics
  async getAnalyticsOverview(): Promise<PlatformOverviewResponse> {
    const res = await apiClient.get<ApiResponse<PlatformOverviewResponse>>('/admin/analytics/overview');
    return res.data.data;
  },

  async getAnalyticsFunnel(params?: {
    jobId?: number;
    companyId?: number;
    dateFrom?: string;
    dateTo?: string;
  }): Promise<ApplicationFunnelResponse> {
    const res = await apiClient.get<ApiResponse<ApplicationFunnelResponse>>('/admin/analytics/applications/funnel', {
      params,
    });
    return res.data.data;
  },

  async getAnalyticsJobs(params?: { dateFrom?: string; dateTo?: string }): Promise<JobAnalyticsResponse> {
    const res = await apiClient.get<ApiResponse<JobAnalyticsResponse>>('/admin/analytics/jobs', { params });
    return res.data.data;
  },

  async getAnalyticsCompanies(): Promise<CompanyAnalyticsResponse> {
    const res = await apiClient.get<ApiResponse<CompanyAnalyticsResponse>>('/admin/analytics/companies');
    return res.data.data;
  },

  async getAnalyticsUsers(): Promise<UserAnalyticsResponse> {
    const res = await apiClient.get<ApiResponse<UserAnalyticsResponse>>('/admin/analytics/users');
    return res.data.data;
  },

  async getAnalyticsTrends(days = 30): Promise<PlatformTrendsResponse> {
    const res = await apiClient.get<ApiResponse<PlatformTrendsResponse>>('/admin/analytics/trends', {
      params: { days },
    });
    return res.data.data;
  },
};
