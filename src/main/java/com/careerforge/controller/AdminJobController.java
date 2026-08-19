package com.careerforge.controller;

import com.careerforge.dto.request.AdminJobModerationRequest;
import com.careerforge.dto.response.AdminJobDetailResponse;
import com.careerforge.dto.response.AdminJobSummaryResponse;
import com.careerforge.dto.response.ApiResponse;
import com.careerforge.entity.enums.JobStatus;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.WorkMode;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.AdminModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminJobController {

    private final AdminModerationService adminModerationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminJobSummaryResponse>>> getJobs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) JobStatus status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) WorkMode workMode,
            @RequestParam(required = false) JobType jobType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdminJobSummaryResponse> jobs = adminModerationService.getJobs(
                search, status, companyId, workMode, jobType, pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Jobs retrieved successfully", jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminJobDetailResponse>> getJobById(@PathVariable Long id) {
        AdminJobDetailResponse jobDetail = adminModerationService.getJobById(id);
        return ResponseEntity.ok(ApiResponse.success("Job details retrieved successfully", jobDetail));
    }

    @PatchMapping("/{id}/moderate")
    public ResponseEntity<ApiResponse<AdminJobDetailResponse>> moderateJob(
            @PathVariable Long id,
            @Valid @RequestBody AdminJobModerationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long currentAdminId = currentUser != null ? currentUser.getId() : null;
        AdminJobDetailResponse moderatedJob = adminModerationService.moderateJob(
                currentAdminId, id, request
        );
        return ResponseEntity.ok(ApiResponse.success("Job moderated successfully", moderatedJob));
    }
}
