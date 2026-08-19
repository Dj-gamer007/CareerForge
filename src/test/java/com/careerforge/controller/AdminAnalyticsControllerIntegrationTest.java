package com.careerforge.controller;

import com.careerforge.entity.*;
import com.careerforge.entity.enums.*;
import com.careerforge.repository.*;
import com.careerforge.security.JwtTokenProvider;
import com.careerforge.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminAnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User adminUser;
    private User recruiterUser;
    private User studentUser;

    private String adminToken;
    private String recruiterToken;
    private String studentToken;

    private Company verifiedCompany;
    private RecruiterProfile recruiterProfile;
    private StudentProfile studentProfile;
    private Job publishedJob;
    private Application application;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .email("admin_analytics_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build());

        recruiterUser = userRepository.save(User.builder()
                .email("recruiter_analytics_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("RecruiterPass123!"))
                .role(Role.ROLE_RECRUITER)
                .enabled(true)
                .build());

        studentUser = userRepository.save(User.builder()
                .email("student_analytics_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("StudentPass123!"))
                .role(Role.ROLE_STUDENT)
                .enabled(true)
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));
        recruiterToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterUser));
        studentToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser));

        verifiedCompany = companyRepository.save(Company.builder()
                .name("Analytics Enterprise")
                .slug("analytics-enterprise")
                .industry("Fintech")
                .location("Bengaluru")
                .companySize("50-200")
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .build());

        recruiterProfile = recruiterProfileRepository.save(RecruiterProfile.builder()
                .user(recruiterUser)
                .company(verifiedCompany)
                .firstName("Sarah")
                .lastName("Connor")
                .designation("Talent Lead")
                .department("Engineering")
                .phone("+919876543210")
                .isCompanyAdmin(true)
                .build());

        studentProfile = studentProfileRepository.save(StudentProfile.builder()
                .user(studentUser)
                .firstName("John")
                .lastName("Doe")
                .phone("+919876543211")
                .build());

        publishedJob = jobRepository.save(Job.builder()
                .company(verifiedCompany)
                .recruiter(recruiterProfile)
                .title("Analytics Platform Engineer")
                .slug("analytics-platform-engineer-1")
                .description("Build big data pipelines")
                .location("Bengaluru")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(new BigDecimal("1500000"))
                .salaryMax(new BigDecimal("2200000"))
                .currency("INR")
                .status(JobStatus.PUBLISHED)
                .deadline(LocalDateTime.now().plusDays(30))
                .publishedAt(LocalDateTime.now())
                .build());

        Resume resume = resumeRepository.save(Resume.builder()
                .studentProfile(studentProfile)
                .originalFileName("resume.pdf")
                .storedFileName("uuid.pdf")
                .storagePath("/uploads/uuid.pdf")
                .fileSize(1024L)
                .contentType("application/pdf")
                .isActive(true)
                .version(1)
                .build());

        application = applicationRepository.save(Application.builder()
                .studentProfile(studentProfile)
                .job(publishedJob)
                .resume(resume)
                .status(ApplicationStatus.UNDER_REVIEW)
                .matchScoreAtApplication(new BigDecimal("85.00"))
                .build());
    }

    // ==========================================
    // RBAC Tests
    // ==========================================

    @Test
    @DisplayName("RBAC - Admin can access all 6 analytics endpoints")
    void testAnalytics_AdminAccess_Allowed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/admin/analytics/applications/funnel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/analytics/jobs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/analytics/companies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/analytics/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/analytics/trends")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("RBAC - Student receiving 403 Forbidden across analytics endpoints")
    void testAnalytics_StudentAccess_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/analytics/trends")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC - Recruiter receiving 403 Forbidden across analytics endpoints")
    void testAnalytics_RecruiterAccess_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC - Unauthenticated request returns 401 Unauthorized")
    void testAnalytics_Unauthenticated_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // Live Database Aggregation Verification Tests
    // ==========================================

    @Test
    @DisplayName("Live DB Aggregation - Platform Overview returns real database counts")
    void testGetPlatformOverview_LiveDb() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.totalCompanies", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalJobs", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalApplications", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.activeApplications", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Live DB Aggregation - Application Funnel returns correct stage counts and percentages")
    void testGetApplicationFunnel_LiveDb() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/applications/funnel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalApplications", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.underReviewCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.activeInPipelinePercentage", greaterThan(0.0)));
    }

    @Test
    @DisplayName("Live DB Aggregation - Job Analytics returns populated enum maps")
    void testGetJobAnalytics_LiveDb() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/jobs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalJobs", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.jobsByStatus.PUBLISHED", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.jobsByStatus.CLOSED", is(0)))
                .andExpect(jsonPath("$.data.jobsByWorkMode.HYBRID", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.jobsByJobType.FULL_TIME", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.jobsByExperienceLevel.MID_LEVEL", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Live DB Aggregation - Company Analytics returns verification breakdown and recruiter stats")
    void testGetCompanyAnalytics_LiveDb() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/companies")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCompanies", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.companiesByVerificationStatus.VERIFIED", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.companiesByVerificationStatus.REJECTED", is(0)))
                .andExpect(jsonPath("$.data.totalRecruiterProfiles", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.averageRecruitersPerCompany", greaterThan(0.0)));
    }

    @Test
    @DisplayName("Live DB Aggregation - User Analytics returns role distribution and profile stats")
    void testGetUserAnalytics_LiveDb() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.usersByRole.ROLE_ADMIN", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.usersByRole.ROLE_STUDENT", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.usersByRole.ROLE_RECRUITER", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalStudentProfiles", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalResumesUploaded", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Live DB Aggregation - Platform Trends returns requested number of daily data points")
    void testGetPlatformTrends_LiveDb() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/trends")
                        .param("days", "14")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.windowDays", is(14)))
                .andExpect(jsonPath("$.data.userRegistrations", hasSize(14)))
                .andExpect(jsonPath("$.data.jobPostings", hasSize(14)))
                .andExpect(jsonPath("$.data.applicationSubmissions", hasSize(14)));
    }

    // ==========================================
    // Validation & Error Handling Tests
    // ==========================================

    @Test
    @DisplayName("Validation - Invalid date range on funnel returns 400 Bad Request")
    void testApplicationFunnel_InvalidDateRange_BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/applications/funnel")
                        .param("dateFrom", "2026-08-20T00:00:00")
                        .param("dateTo", "2026-08-10T00:00:00")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("dateFrom must be before or equal to dateTo")));
    }

    @Test
    @DisplayName("Validation - Invalid days parameter on trends returns 400 Bad Request")
    void testPlatformTrends_InvalidDays_BadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/trends")
                        .param("days", "500")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("days parameter must be between 1 and 365")));

        mockMvc.perform(get("/api/v1/admin/analytics/trends")
                        .param("days", "0")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("days parameter must be between 1 and 365")));
    }
}
