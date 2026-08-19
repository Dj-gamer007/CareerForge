package com.careerforge.controller;

import com.careerforge.dto.request.CompanyVerificationUpdateRequest;
import com.careerforge.dto.response.AdminCompanyDetailResponse;
import com.careerforge.dto.response.AdminCompanySummaryResponse;
import com.careerforge.dto.response.ApiResponse;
import com.careerforge.entity.enums.CompanyVerificationStatus;
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
@RequestMapping("/api/v1/admin/companies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCompanyController {

    private final AdminModerationService adminModerationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminCompanySummaryResponse>>> getCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CompanyVerificationStatus verificationStatus,
            @RequestParam(required = false) String industry,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdminCompanySummaryResponse> companies = adminModerationService.getCompanies(
                search, verificationStatus, industry, pageable
        );
        return ResponseEntity.ok(ApiResponse.success("Companies retrieved successfully", companies));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCompanyDetailResponse>> getCompanyById(@PathVariable Long id) {
        AdminCompanyDetailResponse companyDetail = adminModerationService.getCompanyById(id);
        return ResponseEntity.ok(ApiResponse.success("Company details retrieved successfully", companyDetail));
    }

    @PatchMapping("/{id}/verification")
    public ResponseEntity<ApiResponse<AdminCompanySummaryResponse>> updateCompanyVerification(
            @PathVariable Long id,
            @Valid @RequestBody CompanyVerificationUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long currentAdminId = currentUser != null ? currentUser.getId() : null;
        AdminCompanySummaryResponse updatedCompany = adminModerationService.updateCompanyVerification(
                currentAdminId, id, request
        );
        return ResponseEntity.ok(ApiResponse.success("Company verification status updated successfully", updatedCompany));
    }
}
