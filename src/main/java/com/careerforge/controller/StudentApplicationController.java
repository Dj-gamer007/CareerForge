package com.careerforge.controller;

import com.careerforge.dto.request.ApplicationSubmitRequest;
import com.careerforge.dto.response.ApiResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.dto.response.SkillMatchResponse;
import com.careerforge.dto.response.StudentApplicationDetailResponse;
import com.careerforge.dto.response.StudentApplicationResponse;
import com.careerforge.entity.enums.ApplicationStatus;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.ApplicationService;
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
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/applications")
    public ResponseEntity<ApiResponse<StudentApplicationResponse>> submitApplication(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody ApplicationSubmitRequest request) {
        StudentApplicationResponse response = applicationService.submitApplication(userPrincipal.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Application submitted successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<PagedResponse<StudentApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<StudentApplicationResponse> response = applicationService.getMyApplications(userPrincipal.getId(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Applications retrieved successfully", response));
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApiResponse<StudentApplicationDetailResponse>> getMyApplicationDetail(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        StudentApplicationDetailResponse response = applicationService.getMyApplicationDetail(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Application details retrieved successfully", response));
    }

    @PatchMapping("/applications/{id}/withdraw")
    public ResponseEntity<ApiResponse<StudentApplicationResponse>> withdrawApplication(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long id) {
        StudentApplicationResponse response = applicationService.withdrawApplication(userPrincipal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success("Application withdrawn successfully", response));
    }

    @GetMapping("/jobs/{jobId}/match-preview")
    public ResponseEntity<ApiResponse<SkillMatchResponse>> previewMatchForJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long jobId) {
        SkillMatchResponse response = applicationService.previewMatchForJob(userPrincipal.getId(), jobId);
        return ResponseEntity.ok(ApiResponse.success("Skill match preview calculated successfully", response));
    }
}
