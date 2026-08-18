package com.careerforge.controller;

import com.careerforge.dto.request.JobCreateRequest;
import com.careerforge.dto.request.JobUpdateRequest;
import com.careerforge.dto.response.ApiResponse;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.dto.response.JobSummaryResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiters/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterJobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<ApiResponse<JobDetailResponse>> createJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody JobCreateRequest request) {
        JobDetailResponse job = jobService.createJob(userPrincipal.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Job created successfully as DRAFT", job), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<JobSummaryResponse>>> getCompanyJobs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<JobSummaryResponse> response = jobService.getCompanyJobs(userPrincipal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Company jobs retrieved successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDetailResponse>> getJobDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        JobDetailResponse job = jobService.getJobDetailForRecruiter(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Job retrieved successfully", job));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDetailResponse>> updateJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody JobUpdateRequest request) {
        JobDetailResponse job = jobService.updateJob(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Job updated successfully", job));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<JobDetailResponse>> publishJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        JobDetailResponse job = jobService.publishJob(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Job published successfully", job));
    }

    @PatchMapping("/{id}/unpublish")
    public ResponseEntity<ApiResponse<JobDetailResponse>> unpublishJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        JobDetailResponse job = jobService.unpublishJob(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Job unpublished back to DRAFT", job));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<JobDetailResponse>> closeJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        JobDetailResponse job = jobService.closeJob(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Job closed successfully", job));
    }

    @PatchMapping("/{id}/reopen")
    public ResponseEntity<ApiResponse<JobDetailResponse>> reopenJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        JobDetailResponse job = jobService.reopenJob(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Job reopened successfully", job));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<ApiResponse<JobDetailResponse>> archiveJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        JobDetailResponse job = jobService.archiveJob(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Job archived successfully", job));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        jobService.deleteJob(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Job deleted successfully", null));
    }
}
