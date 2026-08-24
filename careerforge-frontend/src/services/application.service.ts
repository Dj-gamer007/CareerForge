import { apiClient } from '@/lib/axios';
import { ApiResponse, SpringPage } from '@/types/api.types';
import {
  StudentApplicationResponse,
  StudentApplicationDetailResponse,
  RecruiterApplicationResponse,
  RecruiterApplicationDetailResponse,
  ApplicationStatus,
  ApplicationStatusHistoryResponse,
  ApplicationTab,
  ApplicationTabCountsResponse,
} from '@/types/application.types';
import { SavedJobResponse } from '@/types/job.types';

export const applicationService = {
  // Student Applications
  async submitApplication(payload: {
    jobId: number;
    resumeId?: number;
    coverLetter?: string;
  }): Promise<StudentApplicationResponse> {
    const res = await apiClient.post<ApiResponse<StudentApplicationResponse>>('/students/applications', payload);
    return res.data.data;
  },

  async getStudentApplications(params?: {
    status?: ApplicationStatus;
    tab?: ApplicationTab;
    page?: number;
    size?: number;
  }): Promise<SpringPage<StudentApplicationResponse>> {
    const res = await apiClient.get<ApiResponse<SpringPage<StudentApplicationResponse>>>('/students/applications', {
      params,
    });
    return res.data.data;
  },

  async getStudentApplicationCounts(): Promise<ApplicationTabCountsResponse> {
    const res = await apiClient.get<ApiResponse<ApplicationTabCountsResponse>>('/students/applications/counts');
    return res.data.data;
  },

  async getStudentApplicationById(id: number): Promise<StudentApplicationDetailResponse> {
    const res = await apiClient.get<ApiResponse<StudentApplicationDetailResponse>>(`/students/applications/${id}`);
    return res.data.data;
  },

  async withdrawApplication(id: number): Promise<StudentApplicationResponse> {
    const res = await apiClient.patch<ApiResponse<StudentApplicationResponse>>(`/students/applications/${id}/withdraw`);
    return res.data.data;
  },

  // Saved Jobs
  async saveJob(jobId: number): Promise<void> {
    await apiClient.post(`/students/saved-jobs/${jobId}`);
  },

  async removeSavedJob(jobId: number): Promise<void> {
    await apiClient.delete(`/students/saved-jobs/${jobId}`);
  },

  async isJobSaved(jobId: number): Promise<boolean> {
    const res = await apiClient.get<ApiResponse<boolean>>(`/students/saved-jobs/${jobId}/check`);
    return res.data.data;
  },

  async getSavedJobs(params?: { page?: number; size?: number }): Promise<SpringPage<SavedJobResponse>> {
    const res = await apiClient.get<ApiResponse<SpringPage<SavedJobResponse>>>('/students/saved-jobs', {
      params,
    });
    return res.data.data;
  },

  // Recruiter ATS
  async getRecruiterApplications(
    jobId: number,
    params?: {
      status?: ApplicationStatus;
      minScore?: number;
      maxScore?: number;
      search?: string;
      page?: number;
      size?: number;
    }
  ): Promise<SpringPage<RecruiterApplicationResponse>> {
    const res = await apiClient.get<ApiResponse<SpringPage<RecruiterApplicationResponse>>>(
      `/recruiters/jobs/${jobId}/applications`,
      { params }
    );
    return res.data.data;
  },

  async getRecruiterApplicationDetail(id: number): Promise<RecruiterApplicationDetailResponse> {
    const res = await apiClient.get<ApiResponse<RecruiterApplicationDetailResponse>>(`/recruiters/applications/${id}`);
    return res.data.data;
  },

  async updateApplicationStatus(
    id: number,
    payload: {
      status: ApplicationStatus;
      interviewScheduledAt?: string;
    }
  ): Promise<RecruiterApplicationResponse> {
    const res = await apiClient.patch<ApiResponse<RecruiterApplicationResponse>>(
      `/recruiters/applications/${id}/status`,
      payload
    );
    return res.data.data;
  },

  async updateApplicationNotes(id: number, notes: string): Promise<RecruiterApplicationDetailResponse> {
    const res = await apiClient.patch<ApiResponse<RecruiterApplicationDetailResponse>>(
      `/recruiters/applications/${id}/notes`,
      { recruiterNotes: notes, notes }
    );
    return res.data.data;
  },

  async downloadCandidateResume(id: number): Promise<Blob> {
    const res = await apiClient.get(`/recruiters/applications/${id}/resume/download`, {
      responseType: 'blob',
    });
    return res.data;
  },

  async getStudentApplicationHistory(id: number): Promise<ApplicationStatusHistoryResponse[]> {
    const res = await apiClient.get<ApiResponse<ApplicationStatusHistoryResponse[]>>(
      `/students/applications/${id}/history`
    );
    return res.data.data;
  },

  async getRecruiterApplicationHistory(id: number): Promise<ApplicationStatusHistoryResponse[]> {
    const res = await apiClient.get<ApiResponse<ApplicationStatusHistoryResponse[]>>(
      `/recruiters/applications/${id}/history`
    );
    return res.data.data;
  },
};
