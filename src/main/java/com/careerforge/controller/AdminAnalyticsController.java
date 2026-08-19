package com.careerforge.controller;

import com.careerforge.dto.response.ApiResponse;
import com.careerforge.dto.response.analytics.*;
import com.careerforge.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<PlatformOverviewAnalyticsResponse>> getPlatformOverview() {
        PlatformOverviewAnalyticsResponse overview = adminAnalyticsService.getPlatformOverview();
        return ResponseEntity.ok(ApiResponse.success("Platform overview analytics retrieved successfully", overview));
    }

    @GetMapping("/applications/funnel")
    public ResponseEntity<ApiResponse<ApplicationFunnelAnalyticsResponse>> getApplicationFunnel(
            @RequestParam(required = false) Long jobId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
    ) {
        ApplicationFunnelAnalyticsResponse funnel = adminAnalyticsService.getApplicationFunnel(jobId, companyId, dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success("Application funnel analytics retrieved successfully", funnel));
    }

    @GetMapping("/jobs")
    public ResponseEntity<ApiResponse<JobAnalyticsResponse>> getJobAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo
    ) {
        JobAnalyticsResponse jobAnalytics = adminAnalyticsService.getJobAnalytics(dateFrom, dateTo);
        return ResponseEntity.ok(ApiResponse.success("Job analytics retrieved successfully", jobAnalytics));
    }

    @GetMapping("/companies")
    public ResponseEntity<ApiResponse<CompanyAnalyticsResponse>> getCompanyAnalytics() {
        CompanyAnalyticsResponse companyAnalytics = adminAnalyticsService.getCompanyAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Company analytics retrieved successfully", companyAnalytics));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<UserAnalyticsResponse>> getUserAnalytics() {
        UserAnalyticsResponse userAnalytics = adminAnalyticsService.getUserAnalytics();
        return ResponseEntity.ok(ApiResponse.success("User analytics retrieved successfully", userAnalytics));
    }

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<PlatformTrendsAnalyticsResponse>> getPlatformTrends(
            @RequestParam(defaultValue = "30") int days
    ) {
        PlatformTrendsAnalyticsResponse trends = adminAnalyticsService.getPlatformTrends(days);
        return ResponseEntity.ok(ApiResponse.success("Platform trends analytics retrieved successfully", trends));
    }
}
