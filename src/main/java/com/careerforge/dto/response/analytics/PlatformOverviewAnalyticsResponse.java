package com.careerforge.dto.response.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformOverviewAnalyticsResponse {

    // User KPIs
    private long totalUsers;
    private long totalStudents;
    private long totalRecruiters;
    private long totalAdmins;
    private long activeEnabledUsers;
    private long disabledUsers;

    // Company KPIs
    private long totalCompanies;
    private long verifiedCompanies;
    private long pendingCompanies;
    private long rejectedCompanies;

    // Job KPIs
    private long totalJobs;
    private long publishedJobs;
    private long draftJobs;
    private long closedJobs;
    private long archivedJobs;

    // Application KPIs
    private long totalApplications;
    private long activeApplications;
    private long acceptedApplications;
    private long rejectedApplications;
    private long withdrawnApplications;
}
