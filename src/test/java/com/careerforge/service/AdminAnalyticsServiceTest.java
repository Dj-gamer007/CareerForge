package com.careerforge.service;

import com.careerforge.dto.response.analytics.*;
import com.careerforge.entity.enums.*;
import com.careerforge.exception.BadRequestException;
import com.careerforge.repository.*;
import com.careerforge.service.impl.AdminAnalyticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAnalyticsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private StudentProfileRepository studentProfileRepository;

    @Mock
    private RecruiterProfileRepository recruiterProfileRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private AdminAnalyticsServiceImpl adminAnalyticsService;

    @Test
    @DisplayName("Overview Analytics - aggregates platform KPIs and maintains consistency")
    void testGetPlatformOverview_Success() {
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByRole(Role.ROLE_STUDENT)).thenReturn(70L);
        when(userRepository.countByRole(Role.ROLE_RECRUITER)).thenReturn(25L);
        when(userRepository.countByRole(Role.ROLE_ADMIN)).thenReturn(5L);
        when(userRepository.countByEnabled(true)).thenReturn(95L);
        when(userRepository.countByEnabled(false)).thenReturn(5L);

        when(companyRepository.count()).thenReturn(20L);
        when(companyRepository.countByVerificationStatus(CompanyVerificationStatus.VERIFIED)).thenReturn(15L);
        when(companyRepository.countByVerificationStatus(CompanyVerificationStatus.PENDING)).thenReturn(4L);
        when(companyRepository.countByVerificationStatus(CompanyVerificationStatus.REJECTED)).thenReturn(1L);

        when(jobRepository.count()).thenReturn(50L);
        when(jobRepository.countByStatus(JobStatus.PUBLISHED)).thenReturn(30L);
        when(jobRepository.countByStatus(JobStatus.DRAFT)).thenReturn(10L);
        when(jobRepository.countByStatus(JobStatus.CLOSED)).thenReturn(8L);
        when(jobRepository.countByStatus(JobStatus.ARCHIVED)).thenReturn(2L);

        when(applicationRepository.count()).thenReturn(200L);
        when(applicationRepository.countByStatus(ApplicationStatus.ACCEPTED)).thenReturn(20L);
        when(applicationRepository.countByStatus(ApplicationStatus.REJECTED)).thenReturn(40L);
        when(applicationRepository.countByStatus(ApplicationStatus.WITHDRAWN)).thenReturn(10L);

        PlatformOverviewAnalyticsResponse overview = adminAnalyticsService.getPlatformOverview();

        assertThat(overview).isNotNull();
        assertThat(overview.getTotalUsers()).isEqualTo(100L);
        assertThat(overview.getTotalStudents() + overview.getTotalRecruiters() + overview.getTotalAdmins())
                .isEqualTo(overview.getTotalUsers());
        assertThat(overview.getVerifiedCompanies() + overview.getPendingCompanies() + overview.getRejectedCompanies())
                .isEqualTo(overview.getTotalCompanies());
        assertThat(overview.getPublishedJobs() + overview.getDraftJobs() + overview.getClosedJobs() + overview.getArchivedJobs())
                .isEqualTo(overview.getTotalJobs());
        assertThat(overview.getActiveApplications()).isEqualTo(130L); // 200 - (20 + 40 + 10)
    }

    @Test
    @DisplayName("Application Funnel - populates all enum statuses and calculates accurate percentages")
    void testGetApplicationFunnel_Success() {
        List<MetricCountDto<ApplicationStatus>> counts = List.of(
                new MetricCountDto<>(ApplicationStatus.APPLIED, 50L),
                new MetricCountDto<>(ApplicationStatus.UNDER_REVIEW, 30L),
                new MetricCountDto<>(ApplicationStatus.SHORTLISTED, 15L),
                new MetricCountDto<>(ApplicationStatus.INTERVIEW_SCHEDULED, 10L),
                new MetricCountDto<>(ApplicationStatus.ACCEPTED, 5L),
                new MetricCountDto<>(ApplicationStatus.REJECTED, 20L),
                new MetricCountDto<>(ApplicationStatus.WITHDRAWN, 5L)
        );

        when(applicationRepository.countApplicationsGroupedByStatus(null, null, null, null))
                .thenReturn(counts);

        ApplicationFunnelAnalyticsResponse funnel =
                adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel).isNotNull();
        assertThat(funnel.getTotalApplications()).isEqualTo(135L);
        assertThat(funnel.getAppliedCount()).isEqualTo(50L);
        assertThat(funnel.getUnderReviewCount()).isEqualTo(30L);
        assertThat(funnel.getShortlistedCount()).isEqualTo(15L);
        assertThat(funnel.getInterviewScheduledCount()).isEqualTo(10L);
        assertThat(funnel.getAcceptedCount()).isEqualTo(5L);
        assertThat(funnel.getRejectedCount()).isEqualTo(20L);
        assertThat(funnel.getWithdrawnCount()).isEqualTo(5L);

        // Active in pipeline = 50 + 30 + 15 + 10 = 105 / 135 = 77.78%
        assertThat(funnel.getActiveInPipelinePercentage()).isEqualTo(77.78);
        // Acceptance rate = 5 / 135 = 3.70%
        assertThat(funnel.getAcceptanceRatePercentage()).isEqualTo(3.70);
    }

    @Test
    @DisplayName("Application Funnel - handles empty results without division by zero errors")
    void testGetApplicationFunnel_ZeroTotal_HandlesDivideByZero() {
        when(applicationRepository.countApplicationsGroupedByStatus(null, null, null, null))
                .thenReturn(List.of());

        ApplicationFunnelAnalyticsResponse funnel =
                adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel).isNotNull();
        assertThat(funnel.getTotalApplications()).isEqualTo(0L);
        assertThat(funnel.getActiveInPipelinePercentage()).isEqualTo(0.0);
        assertThat(funnel.getAcceptanceRatePercentage()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Application Funnel - invalid date range throws BadRequestException")
    void testGetApplicationFunnel_InvalidDateRange_ThrowsBadRequest() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.minusDays(1);

        assertThatThrownBy(() -> adminAnalyticsService.getApplicationFunnel(null, null, from, to))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("dateFrom must be before or equal to dateTo");
    }

    @Test
    @DisplayName("Job Analytics - populates all enum keys including zero-count constants")
    void testGetJobAnalytics_PopulatesAllEnums() {
        when(jobRepository.countJobsWithDateRange(null, null)).thenReturn(10L);
        when(jobRepository.countJobsGroupedByStatus(null, null)).thenReturn(List.of(
                new MetricCountDto<>(JobStatus.PUBLISHED, 8L),
                new MetricCountDto<>(JobStatus.DRAFT, 2L)
        ));
        when(jobRepository.countJobsGroupedByWorkMode(null, null)).thenReturn(List.of(
                new MetricCountDto<>(WorkMode.REMOTE, 6L),
                new MetricCountDto<>(WorkMode.HYBRID, 4L)
        ));
        when(jobRepository.countJobsGroupedByJobType(null, null)).thenReturn(List.of(
                new MetricCountDto<>(JobType.FULL_TIME, 10L)
        ));
        when(jobRepository.countJobsGroupedByExperienceLevel(null, null)).thenReturn(List.of(
                new MetricCountDto<>(ExperienceLevel.MID_LEVEL, 10L)
        ));

        JobAnalyticsResponse resp = adminAnalyticsService.getJobAnalytics(null, null);

        assertThat(resp).isNotNull();
        assertThat(resp.getTotalJobs()).isEqualTo(10L);
        // Verify missing enum constants are present with 0L
        assertThat(resp.getJobsByStatus().get(JobStatus.CLOSED)).isEqualTo(0L);
        assertThat(resp.getJobsByStatus().get(JobStatus.ARCHIVED)).isEqualTo(0L);
        assertThat(resp.getJobsByWorkMode().get(WorkMode.ONSITE)).isEqualTo(0L);
        assertThat(resp.getJobsByJobType().get(JobType.INTERNSHIP)).isEqualTo(0L);
        assertThat(resp.getJobsByExperienceLevel().get(ExperienceLevel.ENTRY_LEVEL)).isEqualTo(0L);

        // Verify mathematical consistency
        long sumStatus = resp.getJobsByStatus().values().stream().mapToLong(Long::longValue).sum();
        assertThat(sumStatus).isEqualTo(resp.getTotalJobs());
    }

    @Test
    @DisplayName("Company Analytics - calculates average recruiters and populates verification statuses")
    void testGetCompanyAnalytics_Success() {
        when(companyRepository.count()).thenReturn(5L);
        when(companyRepository.countCompaniesGroupedByVerificationStatus()).thenReturn(List.of(
                new MetricCountDto<>(CompanyVerificationStatus.VERIFIED, 3L),
                new MetricCountDto<>(CompanyVerificationStatus.PENDING, 2L)
        ));
        when(companyRepository.countCompaniesGroupedBySize()).thenReturn(List.of(
                new MetricCountDto<>("50-200", 3L),
                new MetricCountDto<>("1-10", 2L)
        ));
        when(recruiterProfileRepository.count()).thenReturn(10L);

        CompanyAnalyticsResponse resp = adminAnalyticsService.getCompanyAnalytics();

        assertThat(resp).isNotNull();
        assertThat(resp.getTotalCompanies()).isEqualTo(5L);
        assertThat(resp.getCompaniesByVerificationStatus().get(CompanyVerificationStatus.REJECTED)).isEqualTo(0L);
        assertThat(resp.getTotalRecruiterProfiles()).isEqualTo(10L);
        assertThat(resp.getAverageRecruitersPerCompany()).isEqualTo(2.0); // 10 / 5 = 2.0
    }

    @Test
    @DisplayName("User Analytics - populates all roles and calculates enabled/disabled users")
    void testGetUserAnalytics_Success() {
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countUsersGroupedByRole()).thenReturn(List.of(
                new MetricCountDto<>(Role.ROLE_STUDENT, 35L),
                new MetricCountDto<>(Role.ROLE_RECRUITER, 10L),
                new MetricCountDto<>(Role.ROLE_ADMIN, 5L)
        ));
        when(userRepository.countByEnabled(true)).thenReturn(48L);
        when(userRepository.countByEnabled(false)).thenReturn(2L);
        when(studentProfileRepository.count()).thenReturn(35L);
        when(resumeRepository.count()).thenReturn(40L);

        UserAnalyticsResponse resp = adminAnalyticsService.getUserAnalytics();

        assertThat(resp).isNotNull();
        assertThat(resp.getTotalUsers()).isEqualTo(50L);
        assertThat(resp.getUsersByRole().get(Role.ROLE_STUDENT)).isEqualTo(35L);
        assertThat(resp.getEnabledUsers()).isEqualTo(48L);
        assertThat(resp.getDisabledUsers()).isEqualTo(2L);
        assertThat(resp.getTotalStudentProfiles()).isEqualTo(35L);
        assertThat(resp.getTotalResumesUploaded()).isEqualTo(40L);

        long sumRoles = resp.getUsersByRole().values().stream().mapToLong(Long::longValue).sum();
        assertThat(sumRoles).isEqualTo(resp.getTotalUsers());
    }

    @Test
    @DisplayName("Platform Trends - invalid days parameter throws BadRequestException")
    void testGetPlatformTrends_InvalidDays_ThrowsBadRequest() {
        assertThatThrownBy(() -> adminAnalyticsService.getPlatformTrends(0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("days parameter must be between 1 and 365");

        assertThatThrownBy(() -> adminAnalyticsService.getPlatformTrends(366))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("days parameter must be between 1 and 365");
    }

    @Test
    @DisplayName("Platform Trends - generates exact day buckets for specified window")
    void testGetPlatformTrends_GeneratesBuckets() {
        when(userRepository.countByCreatedAtBetween(any(), any())).thenReturn(5L);
        when(jobRepository.countByCreatedAtBetween(any(), any())).thenReturn(2L);
        when(applicationRepository.countByCreatedAtBetween(any(), any())).thenReturn(8L);

        PlatformTrendsAnalyticsResponse trends = adminAnalyticsService.getPlatformTrends(7);

        assertThat(trends).isNotNull();
        assertThat(trends.getWindowDays()).isEqualTo(7);
        assertThat(trends.getUserRegistrations()).hasSize(7);
        assertThat(trends.getJobPostings()).hasSize(7);
        assertThat(trends.getApplicationSubmissions()).hasSize(7);
    }
}
