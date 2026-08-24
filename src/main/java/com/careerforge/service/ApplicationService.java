package com.careerforge.service;

import com.careerforge.dto.request.ApplicationNotesRequest;
import com.careerforge.dto.request.ApplicationStatusUpdateRequest;
import com.careerforge.dto.request.ApplicationSubmitRequest;
import com.careerforge.dto.response.*;
import com.careerforge.entity.enums.ApplicationStatus;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ApplicationService {

    // ==========================================
    // Student operations
    // ==========================================
    StudentApplicationResponse submitApplication(Long userId, ApplicationSubmitRequest request);

    PagedResponse<StudentApplicationResponse> getMyApplications(Long userId, ApplicationStatus status, String tab, Pageable pageable);

    default PagedResponse<StudentApplicationResponse> getMyApplications(Long userId, ApplicationStatus status, Pageable pageable) {
        return getMyApplications(userId, status, null, pageable);
    }

    ApplicationTabCountsResponse getStudentApplicationCounts(Long userId);

    StudentApplicationDetailResponse getMyApplicationDetail(Long userId, Long applicationId);

    StudentApplicationResponse withdrawApplication(Long userId, Long applicationId);

    SkillMatchResponse previewMatchForJob(Long userId, Long jobId);

    List<ApplicationStatusHistoryResponse> getApplicationHistoryForStudent(Long userId, Long applicationId);

    // ==========================================
    // Recruiter operations
    // ==========================================
    PagedResponse<RecruiterApplicationSummaryResponse> getJobApplications(
            Long userId,
            Long jobId,
            ApplicationStatus status,
            BigDecimal minScore,
            BigDecimal maxScore,
            String search,
            Pageable pageable);

    RecruiterApplicationDetailResponse getApplicationDetailForRecruiter(Long userId, Long applicationId);

    RecruiterApplicationDetailResponse updateApplicationStatus(Long userId, Long applicationId, ApplicationStatusUpdateRequest request);

    RecruiterApplicationDetailResponse updateApplicationNotes(Long userId, Long applicationId, ApplicationNotesRequest request);

    ResumeService.ResumeDownloadResult downloadApplicantResume(Long userId, Long applicationId);

    List<ApplicationStatusHistoryResponse> getApplicationHistoryForRecruiter(Long userId, Long applicationId);
}
