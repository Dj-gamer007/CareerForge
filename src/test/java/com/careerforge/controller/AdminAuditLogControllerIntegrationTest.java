package com.careerforge.controller;

import com.careerforge.dto.request.AdminJobModerationRequest;
import com.careerforge.dto.request.CompanyVerificationUpdateRequest;
import com.careerforge.dto.request.LoginRequest;
import com.careerforge.dto.request.UserStatusUpdateRequest;
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
class AdminAuditLogControllerIntegrationTest {

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
    private AuditLogRepository auditLogRepository;

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
    private Job publishedJob;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();

        adminUser = userRepository.save(User.builder()
                .email("admin_audit_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build());

        recruiterUser = userRepository.save(User.builder()
                .email("recruiter_audit_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("RecruiterPass123!"))
                .role(Role.ROLE_RECRUITER)
                .enabled(true)
                .build());

        studentUser = userRepository.save(User.builder()
                .email("student_audit_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("StudentPass123!"))
                .role(Role.ROLE_STUDENT)
                .enabled(true)
                .build());

        adminToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));
        recruiterToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterUser));
        studentToken = jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser));

        verifiedCompany = companyRepository.save(Company.builder()
                .name("Audit Enterprise")
                .slug("audit-enterprise")
                .industry("Software")
                .location("Hyderabad")
                .companySize("50-200")
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .build());

        recruiterProfile = recruiterProfileRepository.save(RecruiterProfile.builder()
                .user(recruiterUser)
                .company(verifiedCompany)
                .firstName("Alice")
                .lastName("Walker")
                .designation("Senior Recruiter")
                .department("HR")
                .phone("+919988776655")
                .isCompanyAdmin(true)
                .build());

        publishedJob = jobRepository.save(Job.builder()
                .company(verifiedCompany)
                .recruiter(recruiterProfile)
                .title("Audit Security Architect")
                .slug("audit-sec-arch-123")
                .description("Build secure audit systems")
                .location("Hyderabad")
                .workMode(WorkMode.REMOTE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .salaryMin(new BigDecimal("2000000"))
                .salaryMax(new BigDecimal("3000000"))
                .currency("INR")
                .status(JobStatus.PUBLISHED)
                .deadline(LocalDateTime.now().plusDays(30))
                .publishedAt(LocalDateTime.now())
                .build());
    }

    // ==========================================
    // RBAC & Access Control Tests
    // ==========================================

    @Test
    @DisplayName("RBAC - Admin can access audit log listing")
    void testGetAuditLogs_AdminAccess_Allowed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("RBAC - Student accessing audit logs returns 403 Forbidden")
    void testGetAuditLogs_StudentAccess_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC - Recruiter accessing audit logs returns 403 Forbidden")
    void testGetAuditLogs_RecruiterAccess_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + recruiterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("RBAC - Unauthenticated access to audit logs returns 401 Unauthorized")
    void testGetAuditLogs_Unauthenticated_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // Admin Mutation Audit Integration Tests
    // ==========================================

    @Test
    @DisplayName("Audit Integration - User status update generates USER_STATUS_UPDATED audit record")
    void testUpdateUserStatus_GeneratesAuditRecord() throws Exception {
        UserStatusUpdateRequest req = UserStatusUpdateRequest.builder()
                .enabled(false)
                .reason("Account compliance review")
                .build();

        mockMvc.perform(patch("/api/v1/admin/users/" + studentUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        assertThat(logs).isNotEmpty();

        AuditLog statusLog = logs.stream()
                .filter(l -> l.getEventType() == AuditEventType.USER_STATUS_UPDATED)
                .findFirst()
                .orElse(null);

        assertThat(statusLog).isNotNull();
        assertThat(statusLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(statusLog.getActorEmail()).isEqualTo(adminUser.getEmail());
        assertThat(statusLog.getTargetEntityId()).isEqualTo(studentUser.getId());
        assertThat(statusLog.getReason()).isEqualTo("Account compliance review");
        assertThat(statusLog.getDetails()).contains("\"newEnabled\":false");
    }

    @Test
    @DisplayName("Audit Integration - Company verification generates COMPANY_VERIFICATION_UPDATED audit record")
    void testUpdateCompanyVerification_GeneratesAuditRecord() throws Exception {
        CompanyVerificationUpdateRequest req = CompanyVerificationUpdateRequest.builder()
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .reason("Corporate legal documentation approved")
                .build();

        mockMvc.perform(patch("/api/v1/admin/companies/" + verifiedCompany.getId() + "/verification")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog compLog = logs.stream()
                .filter(l -> l.getEventType() == AuditEventType.COMPANY_VERIFICATION_UPDATED)
                .findFirst()
                .orElse(null);

        assertThat(compLog).isNotNull();
        assertThat(compLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(compLog.getTargetEntityId()).isEqualTo(verifiedCompany.getId());
        assertThat(compLog.getReason()).isEqualTo("Corporate legal documentation approved");
        assertThat(compLog.getDetails()).contains("\"newVerificationStatus\":\"VERIFIED\"");
    }

    @Test
    @DisplayName("Audit Integration - Job moderation generates JOB_MODERATION_PERFORMED audit record")
    void testModerateJob_GeneratesAuditRecord() throws Exception {
        AdminJobModerationRequest req = AdminJobModerationRequest.builder()
                .status(JobStatus.CLOSED)
                .reason("Position closed by admin for regulatory update")
                .build();

        mockMvc.perform(patch("/api/v1/admin/jobs/" + publishedJob.getId() + "/moderate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog jobLog = logs.stream()
                .filter(l -> l.getEventType() == AuditEventType.JOB_MODERATION_PERFORMED)
                .findFirst()
                .orElse(null);

        assertThat(jobLog).isNotNull();
        assertThat(jobLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(jobLog.getTargetEntityId()).isEqualTo(publishedJob.getId());
        assertThat(jobLog.getReason()).isEqualTo("Position closed by admin for regulatory update");
        assertThat(jobLog.getDetails()).contains("\"newStatus\":\"CLOSED\"");
    }

    // ==========================================
    // Rollback Resilience & Failure Audit Tests
    // ==========================================

    @Test
    @DisplayName("Rollback Resilience - Admin self-disable generates USER_SELF_DISABLE_REJECTED failure audit log")
    void testSelfDisable_GeneratesFailureAuditRecord() throws Exception {
        UserStatusUpdateRequest req = UserStatusUpdateRequest.builder()
                .enabled(false)
                .reason("Attempting self-disablement")
                .build();

        mockMvc.perform(patch("/api/v1/admin/users/" + adminUser.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog selfDisableLog = logs.stream()
                .filter(l -> l.getEventType() == AuditEventType.USER_SELF_DISABLE_REJECTED)
                .findFirst()
                .orElse(null);

        assertThat(selfDisableLog).isNotNull();
        assertThat(selfDisableLog.getStatus()).isEqualTo(AuditStatus.FAILURE);
        assertThat(selfDisableLog.getActorEmail()).isEqualTo(adminUser.getEmail());
        assertThat(selfDisableLog.getReason()).isEqualTo("Attempting self-disablement");
        assertThat(selfDisableLog.getDetails()).contains("Administrators cannot disable their own account");
    }

    // ==========================================
    // Authentication Event Tests
    // ==========================================

    @Test
    @DisplayName("Auth Event - Successful admin login creates ADMIN_LOGIN_SUCCESS audit record")
    void testAdminLoginSuccess_CreatesAuditRecord() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("admin_audit_test@careerforge.local")
                .password("AdminPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog loginLog = logs.stream()
                .filter(l -> l.getEventType() == AuditEventType.ADMIN_LOGIN_SUCCESS)
                .findFirst()
                .orElse(null);

        assertThat(loginLog).isNotNull();
        assertThat(loginLog.getStatus()).isEqualTo(AuditStatus.SUCCESS);
        assertThat(loginLog.getActorEmail()).isEqualTo("admin_audit_test@careerforge.local");
        assertThat(loginLog.getActorRole()).isEqualTo("ROLE_ADMIN");
        assertThat(loginLog.getTargetEntityType()).isEqualTo(AuditTargetType.AUTH);
    }

    @Test
    @DisplayName("Auth Event - Failed admin login creates ADMIN_LOGIN_FAILURE audit record")
    void testAdminLoginFailure_CreatesAuditRecord() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("admin_audit_test@careerforge.local")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        List<AuditLog> logs = auditLogRepository.findAll();
        AuditLog failLog = logs.stream()
                .filter(l -> l.getEventType() == AuditEventType.ADMIN_LOGIN_FAILURE)
                .findFirst()
                .orElse(null);

        assertThat(failLog).isNotNull();
        assertThat(failLog.getStatus()).isEqualTo(AuditStatus.FAILURE);
        assertThat(failLog.getActorEmail()).isEqualTo("admin_audit_test@careerforge.local");
        assertThat(failLog.getActorRole()).isEqualTo("ROLE_ADMIN");
        assertThat(failLog.getTargetEntityType()).isEqualTo(AuditTargetType.AUTH);
        // Ensure no password exposure in details or reason
        assertThat(failLog.getDetails()).doesNotContain("WrongPassword123!");
        assertThat(failLog.getReason()).doesNotContain("WrongPassword123!");
    }

    @Test
    @DisplayName("Auth Event - Failed login for non-existent user does NOT create ADMIN_LOGIN_FAILURE")
    void testNonExistentUserLoginFailure_DoesNotCreateAdminAuditRecord() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("nonexistent_unknown_user@careerforge.local")
                .password("RandomPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasAdminFail = logs.stream()
                .anyMatch(l -> l.getEventType() == AuditEventType.ADMIN_LOGIN_FAILURE);

        assertThat(hasAdminFail).isFalse();
    }

    @Test
    @DisplayName("Auth Event - Failed login for student/recruiter does NOT create ADMIN_LOGIN_FAILURE")
    void testStudentLoginFailure_DoesNotCreateAdminAuditRecord() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("student_audit_test@careerforge.local")
                .password("WrongPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());

        List<AuditLog> logs = auditLogRepository.findAll();
        boolean hasAdminFail = logs.stream()
                .anyMatch(l -> l.getEventType() == AuditEventType.ADMIN_LOGIN_FAILURE);

        assertThat(hasAdminFail).isFalse();
    }

    // ==========================================
    // Querying & Detail Inspection Tests
    // ==========================================

    @Test
    @DisplayName("Audit Log Detail - Get by ID returns sanitized detail")
    void testGetAuditLogById_Success() throws Exception {
        AuditLog saved = auditLogRepository.save(AuditLog.builder()
                .actorUserId(adminUser.getId())
                .actorEmail(adminUser.getEmail())
                .actorRole("ROLE_ADMIN")
                .eventType(AuditEventType.USER_STATUS_UPDATED)
                .targetEntityType(AuditTargetType.USER)
                .targetEntityId(studentUser.getId())
                .targetIdentifier(studentUser.getEmail())
                .status(AuditStatus.SUCCESS)
                .reason("Routine audit")
                .details("{\"key\":\"value\"}")
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/v1/admin/audit-logs/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.actorEmail").value(adminUser.getEmail()))
                .andExpect(jsonPath("$.data.details").value("{\"key\":\"value\"}"));
    }

    @Test
    @DisplayName("Audit Log Detail - Non-existent ID returns 404 Not Found")
    void testGetAuditLogById_NotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Audit Log Filtering - Filtering by eventType returns only matching records")
    void testGetAuditLogs_FilterByEventType() throws Exception {
        auditLogRepository.save(AuditLog.builder()
                .actorUserId(adminUser.getId())
                .actorEmail(adminUser.getEmail())
                .actorRole("ROLE_ADMIN")
                .eventType(AuditEventType.USER_STATUS_UPDATED)
                .targetEntityType(AuditTargetType.USER)
                .status(AuditStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());

        auditLogRepository.save(AuditLog.builder()
                .actorUserId(adminUser.getId())
                .actorEmail(adminUser.getEmail())
                .actorRole("ROLE_ADMIN")
                .eventType(AuditEventType.COMPANY_VERIFICATION_UPDATED)
                .targetEntityType(AuditTargetType.COMPANY)
                .status(AuditStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("eventType", "USER_STATUS_UPDATED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].eventType").value("USER_STATUS_UPDATED"));
    }
}
