import { apiClient } from '@/lib/axios';
import { ApiResponse } from '@/types/api.types';
import {
  StudentProfileResponse,
  StudentSkillResponse,
  EducationResponse,
  ProjectResponse,
  CertificationResponse,
  ResumeResponse,
  ProficiencyLevel,
} from '@/types/student.types';

export const studentService = {
  async getProfile(): Promise<StudentProfileResponse> {
    const res = await apiClient.get<ApiResponse<StudentProfileResponse>>('/students/profile');
    return res.data.data;
  },

  async createProfile(payload: {
    firstName: string;
    lastName: string;
    phone?: string;
    location?: string;
    bio?: string;
    educationSummary?: string;
    githubUrl?: string;
    linkedinUrl?: string;
    portfolioUrl?: string;
  }): Promise<StudentProfileResponse> {
    const res = await apiClient.post<ApiResponse<StudentProfileResponse>>('/students/profile', payload);
    return res.data.data;
  },

  async updateProfile(payload: {
    firstName?: string;
    lastName?: string;
    phone?: string;
    location?: string;
    bio?: string;
    educationSummary?: string;
    githubUrl?: string;
    linkedinUrl?: string;
    portfolioUrl?: string;
  }): Promise<StudentProfileResponse> {
    const res = await apiClient.put<ApiResponse<StudentProfileResponse>>('/students/profile', payload);
    return res.data.data;
  },

  // Skills
  async getSkills(): Promise<StudentSkillResponse[]> {
    const res = await apiClient.get<ApiResponse<StudentSkillResponse[]>>('/students/skills');
    return res.data.data;
  },

  async addSkill(payload: { skillId: number; proficiency: ProficiencyLevel }): Promise<StudentSkillResponse> {
    const res = await apiClient.post<ApiResponse<StudentSkillResponse>>('/students/skills', payload);
    return res.data.data;
  },

  async updateSkill(skillId: number, payload: { proficiency: ProficiencyLevel }): Promise<StudentSkillResponse> {
    const res = await apiClient.put<ApiResponse<StudentSkillResponse>>(`/students/skills/${skillId}`, payload);
    return res.data.data;
  },

  async deleteSkill(skillId: number): Promise<void> {
    await apiClient.delete(`/students/skills/${skillId}`);
  },

  // Education
  async getEducation(): Promise<EducationResponse[]> {
    const res = await apiClient.get<ApiResponse<EducationResponse[]>>('/students/education');
    return res.data.data;
  },

  async addEducation(payload: {
    institution: string;
    degree: string;
    fieldOfStudy: string;
    startDate: string;
    endDate?: string;
    current?: boolean;
    grade?: string;
    description?: string;
  }): Promise<EducationResponse> {
    const res = await apiClient.post<ApiResponse<EducationResponse>>('/students/education', payload);
    return res.data.data;
  },

  async updateEducation(
    id: number,
    payload: {
      institution?: string;
      degree?: string;
      fieldOfStudy?: string;
      startDate?: string;
      endDate?: string;
      current?: boolean;
      grade?: string;
      description?: string;
    }
  ): Promise<EducationResponse> {
    const res = await apiClient.put<ApiResponse<EducationResponse>>(`/students/education/${id}`, payload);
    return res.data.data;
  },

  async deleteEducation(id: number): Promise<void> {
    await apiClient.delete(`/students/education/${id}`);
  },

  // Projects
  async getProjects(): Promise<ProjectResponse[]> {
    const res = await apiClient.get<ApiResponse<ProjectResponse[]>>('/students/projects');
    return res.data.data;
  },

  async addProject(payload: {
    title: string;
    description?: string;
    projectUrl?: string;
    githubUrl?: string;
    technologies?: string;
    startDate?: string;
    endDate?: string;
  }): Promise<ProjectResponse> {
    const res = await apiClient.post<ApiResponse<ProjectResponse>>('/students/projects', payload);
    return res.data.data;
  },

  async updateProject(
    id: number,
    payload: {
      title?: string;
      description?: string;
      projectUrl?: string;
      githubUrl?: string;
      technologies?: string;
      startDate?: string;
      endDate?: string;
    }
  ): Promise<ProjectResponse> {
    const res = await apiClient.put<ApiResponse<ProjectResponse>>(`/students/projects/${id}`, payload);
    return res.data.data;
  },

  async deleteProject(id: number): Promise<void> {
    await apiClient.delete(`/students/projects/${id}`);
  },

  // Certifications
  async getCertifications(): Promise<CertificationResponse[]> {
    const res = await apiClient.get<ApiResponse<CertificationResponse[]>>('/students/certifications');
    return res.data.data;
  },

  async addCertification(payload: {
    name: string;
    issuingOrganization: string;
    issueDate?: string;
    expirationDate?: string;
    credentialId?: string;
    credentialUrl?: string;
  }): Promise<CertificationResponse> {
    const res = await apiClient.post<ApiResponse<CertificationResponse>>('/students/certifications', payload);
    return res.data.data;
  },

  async updateCertification(
    id: number,
    payload: {
      name?: string;
      issuingOrganization?: string;
      issueDate?: string;
      expirationDate?: string;
      credentialId?: string;
      credentialUrl?: string;
    }
  ): Promise<CertificationResponse> {
    const res = await apiClient.put<ApiResponse<CertificationResponse>>(`/students/certifications/${id}`, payload);
    return res.data.data;
  },

  async deleteCertification(id: number): Promise<void> {
    await apiClient.delete(`/students/certifications/${id}`);
  },

  // Resumes
  async getResumes(): Promise<ResumeResponse[]> {
    const res = await apiClient.get<ApiResponse<ResumeResponse[]>>('/students/resumes');
    return res.data.data;
  },

  async uploadResume(file: File): Promise<ResumeResponse> {
    const formData = new FormData();
    formData.append('file', file);
    const res = await apiClient.post<ApiResponse<ResumeResponse>>('/students/resumes', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return res.data.data;
  },

  async setActiveResume(id: number): Promise<ResumeResponse> {
    const res = await apiClient.put<ApiResponse<ResumeResponse>>(`/students/resumes/${id}/active`);
    return res.data.data;
  },

  async deleteResume(id: number): Promise<void> {
    await apiClient.delete(`/students/resumes/${id}`);
  },

  async downloadResume(id: number): Promise<Blob> {
    const res = await apiClient.get(`/students/resumes/${id}/download`, {
      responseType: 'blob',
    });
    return res.data;
  },
};
