package com.careerforge.service;

import com.careerforge.dto.response.analytics.*;
import com.careerforge.entity.Application;
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
    @DisplayName("Lifecycle Funnel Scenario 1 - Accepted application counts in all stages")
    void testAcceptedApplication_PassesAllStages() {
        Application app = Application.builder().id(1L).status(ApplicationStatus.ACCEPTED).build();
        when(applicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(app));

        ApplicationFunnelAnalyticsResponse funnel = adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel.getTotalApplications()).isEqualTo(1L);
        assertThat(funnel.getAppliedCount()).isEqualTo(1L);
        assertThat(funnel.getUnderReviewCount()).isEqualTo(1L);
        assertThat(funnel.getShortlistedCount()).isEqualTo(1L);
        assertThat(funnel.getInterviewScheduledCount()).isEqualTo(1L);
        assertThat(funnel.getAcceptedCount()).isEqualTo(1L);
        assertThat(funnel.getRejectedCount()).isEqualTo(0L);
        assertThat(funnel.getWithdrawnCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Lifecycle Funnel Scenario 2 - Rejected after Interview counts in Applied, Review, Shortlist, Interview, and Rejected")
    void testRejectedAfterInterview_PassesInterviewAndPriorStages() {
        Application app = Application.builder().id(2L)
                .status(ApplicationStatus.REJECTED)
                .interviewScheduledAt(LocalDateTime.now().minusDays(2))
                .build();
        when(applicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(app));

        ApplicationFunnelAnalyticsResponse funnel = adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel.getAppliedCount()).isEqualTo(1L);
        assertThat(funnel.getUnderReviewCount()).isEqualTo(1L);
        assertThat(funnel.getShortlistedCount()).isEqualTo(1L);
        assertThat(funnel.getInterviewScheduledCount()).isEqualTo(1L);
        assertThat(funnel.getAcceptedCount()).isEqualTo(0L);
        assertThat(funnel.getRejectedCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Lifecycle Funnel Scenario 3 - Rejected after Shortlist counts in Applied, Review, Shortlist, and Rejected")
    void testRejectedAfterShortlist_PassesShortlistAndPriorStages() {
        Application app = Application.builder().id(3L)
                .status(ApplicationStatus.REJECTED)
                .shortlistedAt(LocalDateTime.now().minusDays(3))
                .build();
        when(applicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(app));

        ApplicationFunnelAnalyticsResponse funnel = adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel.getAppliedCount()).isEqualTo(1L);
        assertThat(funnel.getUnderReviewCount()).isEqualTo(1L);
        assertThat(funnel.getShortlistedCount()).isEqualTo(1L);
        assertThat(funnel.getInterviewScheduledCount()).isEqualTo(0L);
        assertThat(funnel.getRejectedCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Lifecycle Funnel Scenario 4 - Rejected during Under Review counts only in Applied, Review, and Rejected")
    void testRejectedDuringUnderReview_PassesOnlyReviewAndApplied() {
        Application app = Application.builder().id(4L)
                .status(ApplicationStatus.REJECTED)
                .reviewedAt(LocalDateTime.now().minusDays(4))
                .build();
        when(applicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(app));

        ApplicationFunnelAnalyticsResponse funnel = adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel.getAppliedCount()).isEqualTo(1L);
        assertThat(funnel.getUnderReviewCount()).isEqualTo(1L);
        assertThat(funnel.getShortlistedCount()).isEqualTo(0L);
        assertThat(funnel.getInterviewScheduledCount()).isEqualTo(0L);
        assertThat(funnel.getRejectedCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Lifecycle Funnel Scenario 5 - Withdrawn after Interview counts in Applied, Review, Shortlist, Interview, and Withdrawn")
    void testWithdrawnAfterInterview_PassesInterviewAndPriorStages() {
        Application app = Application.builder().id(5L)
                .status(ApplicationStatus.WITHDRAWN)
                .interviewScheduledAt(LocalDateTime.now().minusDays(2))
                .withdrawnAt(LocalDateTime.now())
                .build();
        when(applicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(app));

        ApplicationFunnelAnalyticsResponse funnel = adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel.getAppliedCount()).isEqualTo(1L);
        assertThat(funnel.getUnderReviewCount()).isEqualTo(1L);
        assertThat(funnel.getShortlistedCount()).isEqualTo(1L);
        assertThat(funnel.getInterviewScheduledCount()).isEqualTo(1L);
        assertThat(funnel.getWithdrawnCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Lifecycle Funnel Scenario 6 - Withdrawn at earlier stage (Under Review) counts only in Applied, Review, and Withdrawn")
    void testWithdrawnAtEarlierStage_PassesOnlyReviewAndApplied() {
        Application app = Application.builder().id(6L)
                .status(ApplicationStatus.WITHDRAWN)
                .reviewedAt(LocalDateTime.now().minusDays(5))
                .withdrawnAt(LocalDateTime.now())
                .build();
        when(applicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(app));

        ApplicationFunnelAnalyticsResponse funnel = adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel.getAppliedCount()).isEqualTo(1L);
        assertThat(funnel.getUnderReviewCount()).isEqualTo(1L);
        assertThat(funnel.getShortlistedCount()).isEqualTo(0L);
        assertThat(funnel.getInterviewScheduledCount()).isEqualTo(0L);
        assertThat(funnel.getWithdrawnCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Lifecycle Funnel Scenario 7 - Application that remains Under Review counts in Applied and Review")
    void testApplicationRemainsUnderReview() {
        Application app = Application.builder().id(7L)
                .status(ApplicationStatus.UNDER_REVIEW)
                .reviewedAt(LocalDateTime.now().minusDays(1))
                .build();
        when(applicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(app));

        ApplicationFunnelAnalyticsResponse funnel = adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel.getAppliedCount()).isEqualTo(1L);
        assertThat(funnel.getUnderReviewCount()).isEqualTo(1L);
        assertThat(funnel.getShortlistedCount()).isEqualTo(0L);
        assertThat(funnel.getInterviewScheduledCount()).isEqualTo(0L);
        assertThat(funnel.getAcceptedCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Lifecycle Funnel Scenario 8 - Application that remains Shortlisted counts in Applied, Review, and Shortlist")
    void testApplicationRemainsShortlisted() {
        Application app = Application.builder().id(8L)
                .status(ApplicationStatus.SHORTLISTED)
                .shortlistedAt(LocalDateTime.now().minusDays(1))
                .build();
        when(applicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(app));

        ApplicationFunnelAnalyticsResponse funnel = adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel.getAppliedCount()).isEqualTo(1L);
        assertThat(funnel.getUnderReviewCount()).isEqualTo(1L);
        assertThat(funnel.getShortlistedCount()).isEqualTo(1L);
        assertThat(funnel.getInterviewScheduledCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Lifecycle Funnel Scenario 9 - Mixed 10-application history maintains Applied >= Review >= Shortlist >= Interview and terminal outcomes sum")
    void testMixedLifecycleHistories_MaintainsFunnelInvariantAndTerminalSum() {
        // 7 Accepted, 1 Rejected after Interview, 1 Rejected during Review, 1 Withdrawn during Review = 10 Total
        List<Application> apps = List.of(
                Application.builder().id(1L).status(ApplicationStatus.ACCEPTED).interviewScheduledAt(LocalDateTime.now()).build(),
                Application.builder().id(2L).status(ApplicationStatus.ACCEPTED).interviewScheduledAt(LocalDateTime.now()).build(),
                Application.builder().id(3L).status(ApplicationStatus.ACCEPTED).interviewScheduledAt(LocalDateTime.now()).build(),
                Application.builder().id(4L).status(ApplicationStatus.ACCEPTED).interviewScheduledAt(LocalDateTime.now()).build(),
                Application.builder().id(5L).status(ApplicationStatus.ACCEPTED).interviewScheduledAt(LocalDateTime.now()).build(),
                Application.builder().id(6L).status(ApplicationStatus.ACCEPTED).interviewScheduledAt(LocalDateTime.now()).build(),
                Application.builder().id(7L).status(ApplicationStatus.ACCEPTED).interviewScheduledAt(LocalDateTime.now()).build(),
                Application.builder().id(8L).status(ApplicationStatus.REJECTED).interviewScheduledAt(LocalDateTime.now()).build(), // rejected after interview
                Application.builder().id(9L).status(ApplicationStatus.REJECTED).reviewedAt(LocalDateTime.now()).build(), // rejected in review
                Application.builder().id(10L).status(ApplicationStatus.WITHDRAWN).reviewedAt(LocalDateTime.now()).withdrawnAt(LocalDateTime.now()).build() // withdrawn in review
        );

        when(applicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(apps);

        ApplicationFunnelAnalyticsResponse funnel = adminAnalyticsService.getApplicationFunnel(null, null, null, null);

        assertThat(funnel.getTotalApplications()).isEqualTo(10L);
        assertThat(funnel.getAppliedCount()).isEqualTo(10L);
        assertThat(funnel.getUnderReviewCount()).isEqualTo(10L);
        assertThat(funnel.getShortlistedCount()).isEqualTo(8L);
        assertThat(funnel.getInterviewScheduledCount()).isEqualTo(8L);
        assertThat(funnel.getAcceptedCount()).isEqualTo(7L);
        assertThat(funnel.getRejectedCount()).isEqualTo(2L);
        assertThat(funnel.getWithdrawnCount()).isEqualTo(1L);

        // Verify Invariants: Applied >= Under Review >= Shortlisted >= Interview >= Accepted
        assertThat(funnel.getAppliedCount()).isGreaterThanOrEqualTo(funnel.getUnderReviewCount());
        assertThat(funnel.getUnderReviewCount()).isGreaterThanOrEqualTo(funnel.getShortlistedCount());
        assertThat(funnel.getShortlistedCount()).isGreaterThanOrEqualTo(funnel.getInterviewScheduledCount());
        assertThat(funnel.getInterviewScheduledCount()).isGreaterThanOrEqualTo(funnel.getAcceptedCount());

        // Verify Terminal Sum
        assertThat(funnel.getAcceptedCount() + funnel.getRejectedCount() + funnel.getWithdrawnCount())
                .isEqualTo(funnel.getTotalApplications());
    }

    @Test
    @DisplayName("Application Funnel - handles empty results without division by zero errors")
    void testGetApplicationFunnel_ZeroTotal_HandlesDivideByZero() {
        when(applicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
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
