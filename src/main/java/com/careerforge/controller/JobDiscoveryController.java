package com.careerforge.controller;

import com.careerforge.dto.request.JobSearchCriteria;
import com.careerforge.dto.response.ApiResponse;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.dto.response.JobSummaryResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.entity.enums.ExperienceLevel;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.WorkMode;
import com.careerforge.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobDiscoveryController {

    private final JobService jobService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<JobSummaryResponse>>> searchJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Set<WorkMode> workModes,
            @RequestParam(required = false) Set<JobType> jobTypes,
            @RequestParam(required = false) Set<ExperienceLevel> experienceLevels,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) List<Long> skillIds,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        JobSearchCriteria criteria = JobSearchCriteria.builder()
                .keyword(keyword)
                .location(location)
                .workModes(workModes)
                .jobTypes(jobTypes)
                .experienceLevels(experienceLevels)
                .salaryMin(salaryMin)
                .salaryMax(salaryMax)
                .skillIds(skillIds)
                .companyId(companyId)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        PagedResponse<JobSummaryResponse> response = jobService.searchJobs(criteria);
        return ResponseEntity.ok(ApiResponse.success("Jobs retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDetailResponse>> getJobById(@PathVariable Long id) {
        JobDetailResponse response = jobService.getPublicJobDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Job details retrieved successfully", response));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<JobDetailResponse>> getJobBySlug(@PathVariable String slug) {
        JobDetailResponse response = jobService.getPublicJobDetailBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Job details retrieved successfully", response));
    }
}
