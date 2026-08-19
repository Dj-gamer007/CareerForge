package com.careerforge.dto.response.analytics;

import com.careerforge.entity.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnalyticsResponse {

    private long totalUsers;
    private Map<Role, Long> usersByRole;
    private long enabledUsers;
    private long disabledUsers;
    private long totalStudentProfiles;
    private long totalResumesUploaded;
}
