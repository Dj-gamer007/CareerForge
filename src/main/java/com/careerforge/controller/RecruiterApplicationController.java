package com.careerforge.controller;

import com.careerforge.dto.request.ApplicationNotesRequest;
import com.careerforge.dto.request.ApplicationStatusUpdateRequest;
import com.careerforge.dto.response.*;
import com.careerforge.entity.enums.ApplicationStatus;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.ApplicationService;
import com.careerforge.service.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/recruiters")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/jobs/{jobId}/applications")
    public ResponseEntity<ApiResponse<PagedResponse<RecruiterApplicationSummaryResponse>>> getJobApplications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long jobId,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) BigDecimal minScore,
            @RequestParam(required = false) BigDecimal maxScore,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "matchScoreAtApplication") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<RecruiterApplicationSummaryResponse> response = applicationService.getJobApplications(
                userPrincipal.getId(), jobId, status, minScore, maxScore, search, pageable);

        return ResponseEntity.ok(ApiResponse.success("Job applications retrieved successfully", response));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApiResponse<RecruiterApplicationDetailResponse>> getApplicationDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        RecruiterApplicationDetailResponse response = applicationService.getApplicationDetailForRecruiter(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Application details retrieved successfully", response));
    }

    @GetMapping("/applications/{id}/history")
    public ResponseEntity<ApiResponse<List<ApplicationStatusHistoryResponse>>> getApplicationHistory(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        List<ApplicationStatusHistoryResponse> response = applicationService.getApplicationHistoryForRecruiter(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Application history retrieved successfully", response));
    }

    @PatchMapping("/applications/{id}/status")
    public ResponseEntity<ApiResponse<RecruiterApplicationDetailResponse>> updateApplicationStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        RecruiterApplicationDetailResponse response = applicationService.updateApplicationStatus(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Application status updated successfully", response));
    }

    @PatchMapping("/applications/{id}/notes")
    public ResponseEntity<ApiResponse<RecruiterApplicationDetailResponse>> updateApplicationNotes(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id,
            @Valid @RequestBody ApplicationNotesRequest request) {
        RecruiterApplicationDetailResponse response = applicationService.updateApplicationNotes(userPrincipal.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.success("Recruiter notes updated successfully", response));
    }

    @GetMapping("/applications/{id}/resume/download")
    public ResponseEntity<Resource> downloadApplicantResume(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        ResumeService.ResumeDownloadResult result = applicationService.downloadApplicantResume(userPrincipal.getId(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.originalFileName() + "\"")
                .body(result.resource());
    }
}
