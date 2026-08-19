package com.careerforge.service.impl;

import com.careerforge.dto.response.analytics.*;
import com.careerforge.entity.enums.*;
import com.careerforge.exception.BadRequestException;
import com.careerforge.repository.*;
import com.careerforge.service.AdminAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;

    @Override
    @Transactional(readOnly = true)
    public PlatformOverviewAnalyticsResponse getPlatformOverview() {
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(Role.ROLE_STUDENT);
        long totalRecruiters = userRepository.countByRole(Role.ROLE_RECRUITER);
        long totalAdmins = userRepository.countByRole(Role.ROLE_ADMIN);
        long activeEnabledUsers = userRepository.countByEnabled(true);
        long disabledUsers = userRepository.countByEnabled(false);

        long totalCompanies = companyRepository.count();
        long verifiedCompanies = companyRepository.countByVerificationStatus(CompanyVerificationStatus.VERIFIED);
        long pendingCompanies = companyRepository.countByVerificationStatus(CompanyVerificationStatus.PENDING);
        long rejectedCompanies = companyRepository.countByVerificationStatus(CompanyVerificationStatus.REJECTED);

        long totalJobs = jobRepository.count();
        long publishedJobs = jobRepository.countByStatus(JobStatus.PUBLISHED);
        long draftJobs = jobRepository.countByStatus(JobStatus.DRAFT);
        long closedJobs = jobRepository.countByStatus(JobStatus.CLOSED);
        long archivedJobs = jobRepository.countByStatus(JobStatus.ARCHIVED);

        long totalApplications = applicationRepository.count();
        long acceptedApplications = applicationRepository.countByStatus(ApplicationStatus.ACCEPTED);
        long rejectedApplications = applicationRepository.countByStatus(ApplicationStatus.REJECTED);
        long withdrawnApplications = applicationRepository.countByStatus(ApplicationStatus.WITHDRAWN);
        long activeApplications = totalApplications - (acceptedApplications + rejectedApplications + withdrawnApplications);

        return PlatformOverviewAnalyticsResponse.builder()
                .totalUsers(totalUsers)
                .totalStudents(totalStudents)
                .totalRecruiters(totalRecruiters)
                .totalAdmins(totalAdmins)
                .activeEnabledUsers(activeEnabledUsers)
                .disabledUsers(disabledUsers)
                .totalCompanies(totalCompanies)
                .verifiedCompanies(verifiedCompanies)
                .pendingCompanies(pendingCompanies)
                .rejectedCompanies(rejectedCompanies)
                .totalJobs(totalJobs)
                .publishedJobs(publishedJobs)
                .draftJobs(draftJobs)
                .closedJobs(closedJobs)
                .archivedJobs(archivedJobs)
                .totalApplications(totalApplications)
                .activeApplications(Math.max(0, activeApplications))
                .acceptedApplications(acceptedApplications)
                .rejectedApplications(rejectedApplications)
                .withdrawnApplications(withdrawnApplications)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationFunnelAnalyticsResponse getApplicationFunnel(
            Long jobId,
            Long companyId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo
    ) {
        validateDateRange(dateFrom, dateTo);

        List<MetricCountDto<ApplicationStatus>> statusCounts =
                applicationRepository.countApplicationsGroupedByStatus(jobId, companyId, dateFrom, dateTo);

        Map<ApplicationStatus, Long> map = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            map.put(status, 0L);
        }
        for (MetricCountDto<ApplicationStatus> dto : statusCounts) {
            if (dto.getKey() != null) {
                map.put(dto.getKey(), dto.getCount());
            }
        }

        long appliedCount = map.get(ApplicationStatus.APPLIED);
        long underReviewCount = map.get(ApplicationStatus.UNDER_REVIEW);
        long shortlistedCount = map.get(ApplicationStatus.SHORTLISTED);
        long interviewScheduledCount = map.get(ApplicationStatus.INTERVIEW_SCHEDULED);
        long acceptedCount = map.get(ApplicationStatus.ACCEPTED);
        long rejectedCount = map.get(ApplicationStatus.REJECTED);
        long withdrawnCount = map.get(ApplicationStatus.WITHDRAWN);

        long totalApplications = appliedCount + underReviewCount + shortlistedCount +
                interviewScheduledCount + acceptedCount + rejectedCount + withdrawnCount;

        long activeInPipeline = appliedCount + underReviewCount + shortlistedCount + interviewScheduledCount;

        double activeInPipelinePercentage = calculatePercentage(activeInPipeline, totalApplications);
        double interviewRatePercentage = calculatePercentage(interviewScheduledCount + acceptedCount, totalApplications);
        double acceptanceRatePercentage = calculatePercentage(acceptedCount, totalApplications);
        double rejectionRatePercentage = calculatePercentage(rejectedCount, totalApplications);
        double withdrawalRatePercentage = calculatePercentage(withdrawnCount, totalApplications);

        return ApplicationFunnelAnalyticsResponse.builder()
                .totalApplications(totalApplications)
                .appliedCount(appliedCount)
                .underReviewCount(underReviewCount)
                .shortlistedCount(shortlistedCount)
                .interviewScheduledCount(interviewScheduledCount)
                .acceptedCount(acceptedCount)
                .rejectedCount(rejectedCount)
                .withdrawnCount(withdrawnCount)
                .activeInPipelinePercentage(activeInPipelinePercentage)
                .interviewRatePercentage(interviewRatePercentage)
                .acceptanceRatePercentage(acceptanceRatePercentage)
                .rejectionRatePercentage(rejectionRatePercentage)
                .withdrawalRatePercentage(withdrawalRatePercentage)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public JobAnalyticsResponse getJobAnalytics(LocalDateTime dateFrom, LocalDateTime dateTo) {
        validateDateRange(dateFrom, dateTo);

        long totalJobs = jobRepository.countJobsWithDateRange(dateFrom, dateTo);

        Map<JobStatus, Long> statusMap = new EnumMap<>(JobStatus.class);
        for (JobStatus s : JobStatus.values()) {
            statusMap.put(s, 0L);
        }
        for (MetricCountDto<JobStatus> dto : jobRepository.countJobsGroupedByStatus(dateFrom, dateTo)) {
            if (dto.getKey() != null) {
                statusMap.put(dto.getKey(), dto.getCount());
            }
        }

        Map<WorkMode, Long> workModeMap = new EnumMap<>(WorkMode.class);
        for (WorkMode wm : WorkMode.values()) {
            workModeMap.put(wm, 0L);
        }
        for (MetricCountDto<WorkMode> dto : jobRepository.countJobsGroupedByWorkMode(dateFrom, dateTo)) {
            if (dto.getKey() != null) {
                workModeMap.put(dto.getKey(), dto.getCount());
            }
        }

        Map<JobType, Long> jobTypeMap = new EnumMap<>(JobType.class);
        for (JobType jt : JobType.values()) {
            jobTypeMap.put(jt, 0L);
        }
        for (MetricCountDto<JobType> dto : jobRepository.countJobsGroupedByJobType(dateFrom, dateTo)) {
            if (dto.getKey() != null) {
                jobTypeMap.put(dto.getKey(), dto.getCount());
            }
        }

        Map<ExperienceLevel, Long> expMap = new EnumMap<>(ExperienceLevel.class);
        for (ExperienceLevel el : ExperienceLevel.values()) {
            expMap.put(el, 0L);
        }
        for (MetricCountDto<ExperienceLevel> dto : jobRepository.countJobsGroupedByExperienceLevel(dateFrom, dateTo)) {
            if (dto.getKey() != null) {
                expMap.put(dto.getKey(), dto.getCount());
            }
        }

        return JobAnalyticsResponse.builder()
                .totalJobs(totalJobs)
                .jobsByStatus(statusMap)
                .jobsByWorkMode(workModeMap)
                .jobsByJobType(jobTypeMap)
                .jobsByExperienceLevel(expMap)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CompanyAnalyticsResponse getCompanyAnalytics() {
        long totalCompanies = companyRepository.count();

        Map<CompanyVerificationStatus, Long> statusMap = new EnumMap<>(CompanyVerificationStatus.class);
        for (CompanyVerificationStatus cvs : CompanyVerificationStatus.values()) {
            statusMap.put(cvs, 0L);
        }
        for (MetricCountDto<CompanyVerificationStatus> dto : companyRepository.countCompaniesGroupedByVerificationStatus()) {
            if (dto.getKey() != null) {
                statusMap.put(dto.getKey(), dto.getCount());
            }
        }

        Map<String, Long> sizeMap = new LinkedHashMap<>();
        for (MetricCountDto<String> dto : companyRepository.countCompaniesGroupedBySize()) {
            if (dto.getKey() != null) {
                sizeMap.put(dto.getKey(), dto.getCount());
            }
        }

        long totalRecruiters = recruiterProfileRepository.count();
        double avgRecruiters = totalCompanies == 0 ? 0.0 :
                BigDecimal.valueOf((double) totalRecruiters / totalCompanies)
                        .setScale(2, RoundingMode.HALF_UP)
                        .doubleValue();

        return CompanyAnalyticsResponse.builder()
                .totalCompanies(totalCompanies)
                .companiesByVerificationStatus(statusMap)
                .companiesBySize(sizeMap)
                .totalRecruiterProfiles(totalRecruiters)
                .averageRecruitersPerCompany(avgRecruiters)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserAnalyticsResponse getUserAnalytics() {
        long totalUsers = userRepository.count();

        Map<Role, Long> roleMap = new EnumMap<>(Role.class);
        for (Role r : Role.values()) {
            roleMap.put(r, 0L);
        }
        for (MetricCountDto<Role> dto : userRepository.countUsersGroupedByRole()) {
            if (dto.getKey() != null) {
                roleMap.put(dto.getKey(), dto.getCount());
            }
        }

        long enabledUsers = userRepository.countByEnabled(true);
        long disabledUsers = userRepository.countByEnabled(false);
        long totalStudentProfiles = studentProfileRepository.count();
        long totalResumes = resumeRepository.count();

        return UserAnalyticsResponse.builder()
                .totalUsers(totalUsers)
                .usersByRole(roleMap)
                .enabledUsers(enabledUsers)
                .disabledUsers(disabledUsers)
                .totalStudentProfiles(totalStudentProfiles)
                .totalResumesUploaded(totalResumes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformTrendsAnalyticsResponse getPlatformTrends(int days) {
        if (days < 1 || days > 365) {
            throw new BadRequestException("days parameter must be between 1 and 365");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1);

        List<PlatformTrendsAnalyticsResponse.DailyMetricDto> userRegs = new ArrayList<>();
        List<PlatformTrendsAnalyticsResponse.DailyMetricDto> jobPosts = new ArrayList<>();
        List<PlatformTrendsAnalyticsResponse.DailyMetricDto> appSubmissions = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long userCount = userRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long jobCount = jobRepository.countByCreatedAtBetween(dayStart, dayEnd);
            long appCount = applicationRepository.countByCreatedAtBetween(dayStart, dayEnd);

            userRegs.add(PlatformTrendsAnalyticsResponse.DailyMetricDto.builder()
                    .date(date.toString())
                    .count(userCount)
                    .build());

            jobPosts.add(PlatformTrendsAnalyticsResponse.DailyMetricDto.builder()
                    .date(date.toString())
                    .count(jobCount)
                    .build());

            appSubmissions.add(PlatformTrendsAnalyticsResponse.DailyMetricDto.builder()
                    .date(date.toString())
                    .count(appCount)
                    .build());
        }

        return PlatformTrendsAnalyticsResponse.builder()
                .windowDays(days)
                .userRegistrations(userRegs)
                .jobPostings(jobPosts)
                .applicationSubmissions(appSubmissions)
                .build();
    }

    private void validateDateRange(LocalDateTime dateFrom, LocalDateTime dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BadRequestException("dateFrom must be before or equal to dateTo");
        }
    }

    private double calculatePercentage(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf((double) numerator / denominator * 100.0)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
