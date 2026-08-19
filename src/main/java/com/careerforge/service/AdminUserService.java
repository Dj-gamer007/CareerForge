package com.careerforge.service;

import com.careerforge.dto.request.UserStatusUpdateRequest;
import com.careerforge.dto.response.AdminUserDetailResponse;
import com.careerforge.dto.response.AdminUserSummaryResponse;
import com.careerforge.entity.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserSummaryResponse> getUsers(String search, Role role, Boolean enabled, Pageable pageable);

    AdminUserDetailResponse getUserById(Long id);

    AdminUserSummaryResponse updateUserStatus(Long currentAdminId, Long targetUserId, UserStatusUpdateRequest request);
}
