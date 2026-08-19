package com.careerforge.service;

import com.careerforge.dto.request.AdminJobModerationRequest;
import com.careerforge.dto.request.CompanyVerificationUpdateRequest;
import com.careerforge.dto.response.*;
import com.careerforge.entity.enums.CompanyVerificationStatus;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.WorkMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminModerationService {

    // ==========================================
    // Company Verification & Inspection
    // ==========================================

    Page<AdminCompanySummaryResponse> getCompanies(
            String search,
            CompanyVerificationStatus verificationStatus,
            String industry,
            Pageable pageable
    );

    AdminCompanyDetailResponse getCompanyById(Long id);

    AdminCompanySummaryResponse updateCompanyVerification(
            Long adminUserId,
            Long companyId,
            CompanyVerificationUpdateRequest request
    );

    // ==========================================
    // Job Moderation & Inspection
    // ==========================================

    Page<AdminJobSummaryResponse> getJobs(
            String search,
            JobStatus status,
            Long companyId,
            WorkMode workMode,
            JobType jobType,
            Pageable pageable
    );

    AdminJobDetailResponse getJobById(Long id);

    AdminJobDetailResponse moderateJob(
            Long adminUserId,
            Long jobId,
            AdminJobModerationRequest request
    );
}
