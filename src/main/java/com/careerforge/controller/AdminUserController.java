package com.careerforge.controller;

import com.careerforge.dto.request.UserStatusUpdateRequest;
import com.careerforge.dto.response.AdminUserDetailResponse;
import com.careerforge.dto.response.AdminUserSummaryResponse;
import com.careerforge.dto.response.ApiResponse;
import com.careerforge.entity.enums.Role;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.AdminUserService;
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
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserSummaryResponse>>> getUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = sortDirection.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<AdminUserSummaryResponse> users = adminUserService.getUsers(search, role, enabled, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserById(@PathVariable Long id) {
        AdminUserDetailResponse userDetail = adminUserService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", userDetail));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserSummaryResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        Long currentAdminId = currentUser != null ? currentUser.getId() : null;
        AdminUserSummaryResponse updatedUser = adminUserService.updateUserStatus(currentAdminId, id, request);
        return ResponseEntity.ok(ApiResponse.success("User account status updated successfully", updatedUser));
    }
}
