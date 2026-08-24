package com.careerforge.controller;

import com.careerforge.dto.request.AdminJobModerationRequest;
import com.careerforge.dto.request.CompanyVerificationUpdateRequest;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.*;
import com.careerforge.repository.*;
import com.careerforge.security.JwtTokenProvider;
import com.careerforge.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminModerationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private JobSkillRepository jobSkillRepository;

    @Autowired
    private NotificationRepository notificationRepository;

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
    private Company pendingCompany;
    private RecruiterProfile recruiterProfile;
    private Job publishedJob;
    private Skill testSkill;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.save(User.builder()
                .email("admin_mod_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build());

        recruiterUser = userRepository.save(User.builder()
                .email("recruiter_mod_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("RecruiterPass123!"))
                .role(Role.ROLE_RECRUITER)
                .enabled(true)
                .build());

        studentUser = userRepository.save(User.builder()
                .email("student_mod_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("StudentPass123!"))
                .role(Role.ROLE_STUDENT)
                .enabled(true)
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));
        recruiterToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterUser));
        studentToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser));

        verifiedCompany = companyRepository.save(Company.builder()
                .name("Apex Innovations")
                .slug("apex-innovations")
                .industry("Cloud Computing")
                .location("Hyderabad")
                .companySize("50-200")
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .build());

        pendingCompany = companyRepository.save(Company.builder()
                .name("Beta Startup")
                .slug("beta-startup")
                .industry("Fintech")
                .location("Mumbai")
                .companySize("1-10")
                .verificationStatus(CompanyVerificationStatus.PENDING)
                .build());

        recruiterProfile = recruiterProfileRepository.save(RecruiterProfile.builder()
                .user(recruiterUser)
                .company(verifiedCompany)
                .firstName("Robert")
                .lastName("Taylor")
                .designation("Technical Recruiter")
                .department("Talent Acquisition")
                .phone("+919123456780")
                .isCompanyAdmin(true)
                .build());

        testSkill = skillRepository.save(Skill.builder()
                .name("Java Backend Mod")
                .category("Backend")
                .build());

        publishedJob = jobRepository.save(Job.builder()
                .company(verifiedCompany)
                .recruiter(recruiterProfile)
                .title("Senior Cloud Architect")
                .slug("senior-cloud-architect-xyz")
                .description("Design high-throughput microservices")
                .location("Hyderabad")
                .workMode(WorkMode.REMOTE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .salaryMin(new BigDecimal("2500000"))
                .salaryMax(new BigDecimal("3500000"))
                .currency("INR")
                .status(JobStatus.PUBLISHED)
                .deadline(LocalDateTime.now().plusDays(30))
                .publishedAt(LocalDateTime.now())
                .build());

        jobSkillRepository.save(JobSkill.builder()
                .job(publishedJob)
                .skill(testSkill)
                .isRequired(true)
                .minimumProficiency(SkillProficiency.EXPERT)
                .build());
    }

    // ==========================================
    // Company RBAC & Endpoints
    // ==========================================

    @Test
    @DisplayName("Admin company listing - Student receives 403 Forbidden")
    void testAdminCompanyRBAC_StudentForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/companies")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin company listing - Recruiter receives 403 Forbidden")
    void testAdminCompanyRBAC_RecruiterForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/companies")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin company listing - Unauthenticated request receives 401 Unauthorized")
    void testAdminCompanyRBAC_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/companies"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Admin company listing - Filter by verificationStatus returns matching companies")
    void testGetCompanies_Admin_FilterByVerificationStatus() throws Exception {
        mockMvc.perform(get("/api/v1/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("verificationStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].verificationStatus").value("PENDING"));
    }

    @Test
    @DisplayName("Admin company listing - Search by keyword returns matched company")
    void testGetCompanies_Admin_Search() throws Exception {
        mockMvc.perform(get("/api/v1/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "Apex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].name").value("Apex Innovations"))
                .andExpect(jsonPath("$.data.content[0].totalJobsCount").value(greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Admin company detail - Returns detailed company with recruiter roster")
    void testGetCompanyDetail_Admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/companies/" + verifiedCompany.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(verifiedCompany.getId()))
                .andExpect(jsonPath("$.data.name").value("Apex Innovations"))
                .andExpect(jsonPath("$.data.recruiters", hasSize(1)))
                .andExpect(jsonPath("$.data.recruiters[0].email").value("recruiter_mod_test@careerforge.local"))
                .andExpect(jsonPath("$.data.recruiters[0].firstName").value("Robert"));
    }

    @Test
    @DisplayName("Admin company detail - Nonexistent company returns 404 Not Found")
    void testGetCompanyDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/companies/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Update company verification - Approve pending company dispatches notification")
    void testUpdateCompanyVerification_Approve() throws Exception {
        CompanyVerificationUpdateRequest request = CompanyVerificationUpdateRequest.builder()
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .reason("Verified certificates and tax documents")
                .build();

        mockMvc.perform(patch("/api/v1/admin/companies/" + pendingCompany.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));
    }

    @Test
    @DisplayName("Update company verification - Missing reason returns 400 Bad Request")
    void testUpdateCompanyVerification_MissingReason() throws Exception {
        CompanyVerificationUpdateRequest request = CompanyVerificationUpdateRequest.builder()
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .reason("")
                .build();

        mockMvc.perform(patch("/api/v1/admin/companies/" + pendingCompany.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Update company verification - Reject pending company dispatches notification and updates status to REJECTED")
    void testUpdateCompanyVerification_Reject() throws Exception {
        CompanyVerificationUpdateRequest request = CompanyVerificationUpdateRequest.builder()
                .verificationStatus(CompanyVerificationStatus.REJECTED)
                .reason("Invalid corporate registration documents")
                .build();

        mockMvc.perform(patch("/api/v1/admin/companies/" + pendingCompany.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verificationStatus").value("REJECTED"));
    }

    @Test
    @DisplayName("Recruiter registers company - Defaults to PENDING, appears in Admin pending list, and enforces publish guard")
    void testRecruiterRegisterCompany_LifecycleFlow() throws Exception {
        // 1. Create a new unassociated recruiter
        User newRecruiterUser = userRepository.save(User.builder()
                .email("unassociated_recruiter@careerforge.local")
                .passwordHash(passwordEncoder.encode("RecruiterPass123!"))
                .role(Role.ROLE_RECRUITER)
                .enabled(true)
                .build());
        String newRecruiterToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(newRecruiterUser));

        com.careerforge.dto.request.CompanyCreateRequest createRequest = com.careerforge.dto.request.CompanyCreateRequest.builder()
                .name("Beta Solutions Inc")
                .industry("Fintech")
                .companySize("11-50")
                .location("Mumbai")
                .build();

        // 2. Recruiter registers company -> must be PENDING
        String responseBody = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", "Bearer " + newRecruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Beta Solutions Inc"))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"))
                .andReturn().getResponse().getContentAsString();

        long createdCompanyId = objectMapper.readTree(responseBody).path("data").path("id").asLong();

        // 3. Admin queries PENDING companies -> newly registered company is present
        mockMvc.perform(get("/api/v1/admin/companies")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("verificationStatus", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].id", hasItem((int) createdCompanyId)))
                .andExpect(jsonPath("$.data.content[*].verificationStatus", hasItem("PENDING")));

        // 4. Create a draft job for this pending company
        com.careerforge.dto.request.JobCreateRequest jobRequest = com.careerforge.dto.request.JobCreateRequest.builder()
                .title("Associate QA Engineer")
                .description("Quality assurance testing")
                .workMode(WorkMode.ONSITE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.ENTRY_LEVEL)
                .skills(List.of(com.careerforge.dto.request.JobSkillItemRequest.builder()
                        .skillId(testSkill.getId())
                        .isRequired(true)
                        .minimumProficiency(SkillProficiency.INTERMEDIATE)
                        .build()))
                .deadline(LocalDateTime.now().plusDays(15))
                .build();

        String jobResp = mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", "Bearer " + newRecruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        long createdJobId = objectMapper.readTree(jobResp).path("data").path("id").asLong();

        // 5. Recruiter tries to PUBLISH job while company is PENDING -> must fail with 400 Bad Request
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + createdJobId + "/publish")
                        .header("Authorization", "Bearer " + newRecruiterToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot publish jobs for an unverified company")));

        // 6. Admin approves company -> status becomes VERIFIED
        CompanyVerificationUpdateRequest approveRequest = CompanyVerificationUpdateRequest.builder()
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .reason("Official registration verified")
                .build();

        mockMvc.perform(patch("/api/v1/admin/companies/" + createdCompanyId + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(approveRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"));

        // 7. Recruiter can now successfully PUBLISH the job
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + createdJobId + "/publish")
                        .header("Authorization", "Bearer " + newRecruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    // ==========================================
    // Job Moderation RBAC & Endpoints
    // ==========================================

    @Test
    @DisplayName("Admin job listing - Student receives 403 Forbidden")
    void testAdminJobRBAC_StudentForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/jobs")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin job listing - Recruiter receives 403 Forbidden")
    void testAdminJobRBAC_RecruiterForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/jobs")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin job listing - Filter by status and search keyword")
    void testGetJobs_Admin_SearchAndStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/jobs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("search", "Architect")
                        .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Senior Cloud Architect"))
                .andExpect(jsonPath("$.data.content[0].companyName").value("Apex Innovations"))
                .andExpect(jsonPath("$.data.content[0].skills", hasSize(1)));
    }

    @Test
    @DisplayName("Admin job detail - Returns detailed job response")
    void testGetJobDetail_Admin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/jobs/" + publishedJob.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(publishedJob.getId()))
                .andExpect(jsonPath("$.data.title").value("Senior Cloud Architect"))
                .andExpect(jsonPath("$.data.companyName").value("Apex Innovations"))
                .andExpect(jsonPath("$.data.recruiterName").value("Robert Taylor"))
                .andExpect(jsonPath("$.data.recruiterEmail").value("recruiter_mod_test@careerforge.local"))
                .andExpect(jsonPath("$.data.skills", hasSize(1)));
    }

    @Test
    @DisplayName("Moderate job - Force close published job transitions status and notifies recruiter")
    void testModerateJob_ForceClose() throws Exception {
        AdminJobModerationRequest request = AdminJobModerationRequest.builder()
                .status(JobStatus.CLOSED)
                .reason("Salary out of compliance")
                .build();

        mockMvc.perform(patch("/api/v1/admin/jobs/" + publishedJob.getId() + "/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // Verify notification dispatched
        long notifCount = notificationRepository.count();
        assertThat(notifCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Moderate job - Force archive job transitions status")
    void testModerateJob_ForceArchive() throws Exception {
        AdminJobModerationRequest request = AdminJobModerationRequest.builder()
                .status(JobStatus.ARCHIVED)
                .reason("Expired and non-responsive listing")
                .build();

        mockMvc.perform(patch("/api/v1/admin/jobs/" + publishedJob.getId() + "/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));
    }

    @Test
    @DisplayName("Moderate job - Return closed job to draft transitions status")
    void testModerateJob_ReturnToDraft() throws Exception {
        publishedJob.setStatus(JobStatus.CLOSED);
        jobRepository.save(publishedJob);

        AdminJobModerationRequest request = AdminJobModerationRequest.builder()
                .status(JobStatus.DRAFT)
                .reason("Please correct the compensation figures and republish")
                .build();

        mockMvc.perform(patch("/api/v1/admin/jobs/" + publishedJob.getId() + "/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    @DisplayName("Moderate job - Invalid transition (DRAFT -> PUBLISHED) returns 400 Bad Request")
    void testModerateJob_InvalidTransition() throws Exception {
        publishedJob.setStatus(JobStatus.DRAFT);
        jobRepository.save(publishedJob);

        AdminJobModerationRequest request = AdminJobModerationRequest.builder()
                .status(JobStatus.PUBLISHED)
                .reason("Cannot publish directly via moderation")
                .build();

        mockMvc.perform(patch("/api/v1/admin/jobs/" + publishedJob.getId() + "/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid admin moderation transition")));
    }

    @Test
    @DisplayName("Publishing Guard - Recruiter belonging to unverified company cannot publish job")
    void testUnverifiedCompanyCannotPublishJob() throws Exception {
        User pendingRecruiterUser = userRepository.save(User.builder()
                .email("pending_recruiter@careerforge.local")
                .passwordHash(passwordEncoder.encode("RecruiterPass123!"))
                .role(Role.ROLE_RECRUITER)
                .enabled(true)
                .build());

        String pendingRecruiterToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(pendingRecruiterUser));

        // Create draft job under unverified/pending company
        RecruiterProfile pendingRecruiter = recruiterProfileRepository.save(RecruiterProfile.builder()
                .user(pendingRecruiterUser)
                .company(pendingCompany)
                .firstName("Robert")
                .lastName("Taylor")
                .designation("Talent Specialist")
                .department("Recruiting")
                .phone("+919123456789")
                .isCompanyAdmin(true)
                .build());

        Job draftJob = jobRepository.save(Job.builder()
                .company(pendingCompany)
                .recruiter(pendingRecruiter)
                .title("Junior Dev")
                .slug("junior-dev-xyz")
                .description("Junior software engineer")
                .location("Mumbai")
                .workMode(WorkMode.ONSITE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.ENTRY_LEVEL)
                .salaryMin(new BigDecimal("600000"))
                .salaryMax(new BigDecimal("800000"))
                .currency("INR")
                .status(JobStatus.DRAFT)
                .deadline(LocalDateTime.now().plusDays(30))
                .build());

        jobSkillRepository.save(JobSkill.builder()
                .job(draftJob)
                .skill(testSkill)
                .isRequired(true)
                .minimumProficiency(SkillProficiency.BEGINNER)
                .build());

        // Recruiter tries to publish draft job
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + draftJob.getId() + "/publish")
                        .header("Authorization", "Bearer " + pendingRecruiterToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Cannot publish jobs for an unverified company")));
    }
}
