package com.careerforge.controller;

import com.careerforge.dto.response.ApiResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.dto.response.SavedJobResponse;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.SavedJobService;
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
@RequestMapping("/api/v1/students/saved-jobs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentSavedJobController {

    private final SavedJobService savedJobService;

    @PostMapping("/{jobId}")
    public ResponseEntity<ApiResponse<SavedJobResponse>> saveJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long jobId) {
        SavedJobResponse response = savedJobService.saveJob(userPrincipal.getId(), jobId);
        return new ResponseEntity<>(ApiResponse.success("Job bookmarked successfully", response), HttpStatus.CREATED);
    }

    @DeleteMapping("/{jobId}")
    public ResponseEntity<ApiResponse<Void>> removeSavedJob(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long jobId) {
        savedJobService.removeSavedJob(userPrincipal.getId(), jobId);
        return ResponseEntity.ok(ApiResponse.success("Job removed from bookmarks", null));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<SavedJobResponse>>> getSavedJobs(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        PagedResponse<SavedJobResponse> response = savedJobService.getSavedJobs(userPrincipal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Saved jobs retrieved successfully", response));
    }

    @GetMapping("/{jobId}/check")
    public ResponseEntity<ApiResponse<Boolean>> checkIsJobSaved(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @PathVariable Long jobId) {
        boolean isSaved = savedJobService.isJobSaved(userPrincipal.getId(), jobId);
        return ResponseEntity.ok(ApiResponse.success("Saved job check status", isSaved));
    }
}
