package com.careerforge.controller;

import com.careerforge.dto.request.RecruiterProfileRequest;
import com.careerforge.dto.response.ApiResponse;
import com.careerforge.dto.response.RecruiterProfileResponse;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.RecruiterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiters")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterController {

    private final RecruiterService recruiterService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        RecruiterProfileResponse profile = recruiterService.getProfileByUserId(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success("Recruiter profile retrieved successfully", profile));
    }

    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> createProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody RecruiterProfileRequest request) {
        RecruiterProfileResponse profile = recruiterService.createProfile(userPrincipal.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Recruiter profile created successfully", profile), HttpStatus.CREATED);
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<RecruiterProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody RecruiterProfileRequest request) {
        RecruiterProfileResponse profile = recruiterService.updateProfile(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Recruiter profile updated successfully", profile));
    }
}
