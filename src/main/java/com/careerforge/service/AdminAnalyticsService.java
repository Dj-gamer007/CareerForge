package com.careerforge.service;

import com.careerforge.dto.response.analytics.*;

import java.time.LocalDateTime;

public interface AdminAnalyticsService {

    PlatformOverviewAnalyticsResponse getPlatformOverview();

    ApplicationFunnelAnalyticsResponse getApplicationFunnel(Long jobId, Long companyId, LocalDateTime dateFrom, LocalDateTime dateTo);

    JobAnalyticsResponse getJobAnalytics(LocalDateTime dateFrom, LocalDateTime dateTo);

    CompanyAnalyticsResponse getCompanyAnalytics();

    UserAnalyticsResponse getUserAnalytics();

    PlatformTrendsAnalyticsResponse getPlatformTrends(int days);
}
