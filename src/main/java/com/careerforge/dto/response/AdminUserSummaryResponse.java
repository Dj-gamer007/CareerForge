package com.careerforge.dto.response;

import com.careerforge.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryResponse {

    private Long id;
    private String email;
    private Role role;
    private boolean enabled;
    private String fullName;
    private String profileType; // "STUDENT", "RECRUITER", or "NONE"
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
