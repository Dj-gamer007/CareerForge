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

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashSet;
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
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) Set<JobType> jobTypes,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(required = false) Set<ExperienceLevel> experienceLevels,
            @RequestParam(required = false) ExperienceLevel experienceLevel,
            @RequestParam(required = false) BigDecimal salaryMin,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) BigDecimal salaryMax,
            @RequestParam(required = false) BigDecimal maxSalary,
            @RequestParam(required = false) List<Long> skillIds,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Set<WorkMode> resolvedWorkModes = workModes != null ? new HashSet<>(workModes) : new HashSet<>();
        if (workMode != null) {
            resolvedWorkModes.add(workMode);
        }

        Set<JobType> resolvedJobTypes = jobTypes != null ? new HashSet<>(jobTypes) : new HashSet<>();
        if (jobType != null) {
            resolvedJobTypes.add(jobType);
        }

        Set<ExperienceLevel> resolvedExpLevels = experienceLevels != null ? new HashSet<>(experienceLevels) : new HashSet<>();
        if (experienceLevel != null) {
            resolvedExpLevels.add(experienceLevel);
        }

        BigDecimal resolvedSalaryMin = salaryMin != null ? salaryMin : minSalary;
        BigDecimal resolvedSalaryMax = salaryMax != null ? salaryMax : maxSalary;

        String resolvedSortBy = sortBy;
        String resolvedSortDir = sortDirection;
        if (StringUtils.hasText(sort) && sort.contains(",")) {
            String[] parts = sort.split(",");
            if (parts.length >= 1 && StringUtils.hasText(parts[0])) {
                resolvedSortBy = parts[0].trim();
            }
            if (parts.length >= 2 && StringUtils.hasText(parts[1])) {
                resolvedSortDir = parts[1].trim();
            }
        }

        JobSearchCriteria criteria = JobSearchCriteria.builder()
                .keyword(keyword)
                .location(location)
                .workModes(resolvedWorkModes.isEmpty() ? null : resolvedWorkModes)
                .jobTypes(resolvedJobTypes.isEmpty() ? null : resolvedJobTypes)
                .experienceLevels(resolvedExpLevels.isEmpty() ? null : resolvedExpLevels)
                .salaryMin(resolvedSalaryMin)
                .salaryMax(resolvedSalaryMax)
                .skillIds(skillIds)
                .companyId(companyId)
                .page(page)
                .size(size)
                .sortBy(resolvedSortBy)
                .sortDirection(resolvedSortDir)
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
