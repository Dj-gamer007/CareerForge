package com.careerforge.controller;

import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.CompanyUpdateRequest;
import com.careerforge.dto.response.ApiResponse;
import com.careerforge.dto.response.CompanyResponse;
import com.careerforge.dto.response.CompanySummaryResponse;
import com.careerforge.dto.response.PagedResponse;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CompanyCreateRequest request) {
        CompanyResponse company = companyService.createCompany(userPrincipal.getId(), request);
        return new ResponseEntity<>(ApiResponse.success("Company created successfully", company), HttpStatus.CREATED);
    }

    @GetMapping("/my-company")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyResponse>> getMyCompany(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        CompanyResponse company = companyService.getMyCompany(userPrincipal.getId());
        if (company == null) {
            return ResponseEntity.ok(ApiResponse.success("Recruiter has no company registered yet.", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Company retrieved successfully", company));
    }

    @PutMapping("/my-company")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<CompanyResponse>> updateMyCompany(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody CompanyUpdateRequest request) {
        CompanyResponse company = companyService.updateMyCompany(userPrincipal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Company updated successfully", company));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyById(@PathVariable Long id) {
        CompanyResponse company = companyService.getCompanyById(id);
        return ResponseEntity.ok(ApiResponse.success("Company retrieved successfully", company));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompanyBySlug(@PathVariable String slug) {
        CompanyResponse company = companyService.getCompanyBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success("Company retrieved successfully", company));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CompanySummaryResponse>>> getVerifiedCompanies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String searchTerm = StringUtils.hasText(search) ? search : name;
        PagedResponse<CompanySummaryResponse> response = companyService.getVerifiedCompanies(searchTerm, pageable);
        return ResponseEntity.ok(ApiResponse.success("Companies retrieved successfully", response));
    }
}
