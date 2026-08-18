package com.careerforge.service;

import com.careerforge.dto.request.JobCreateRequest;
import com.careerforge.dto.request.JobSearchCriteria;
import com.careerforge.dto.request.JobUpdateRequest;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.dto.response.JobSummaryResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.entity.enums.JobStatus;
import org.springframework.data.domain.Pageable;

public interface JobService {

    // Recruiter operations
    JobDetailResponse createJob(Long userId, JobCreateRequest request);

    PagedResponse<JobSummaryResponse> getCompanyJobs(Long userId, JobStatus status, Pageable pageable);

    JobDetailResponse getJobDetailForRecruiter(Long userId, Long jobId);

    JobDetailResponse updateJob(Long userId, Long jobId, JobUpdateRequest request);

    JobDetailResponse publishJob(Long userId, Long jobId);

    JobDetailResponse unpublishJob(Long userId, Long jobId);

    JobDetailResponse closeJob(Long userId, Long jobId);

    JobDetailResponse reopenJob(Long userId, Long jobId);

    JobDetailResponse archiveJob(Long userId, Long jobId);

    void deleteJob(Long userId, Long jobId);

    // Public / candidate discovery
    PagedResponse<JobSummaryResponse> searchJobs(JobSearchCriteria criteria);

    JobDetailResponse getPublicJobDetail(Long jobId);

    JobDetailResponse getPublicJobDetailBySlug(String slug);
}
