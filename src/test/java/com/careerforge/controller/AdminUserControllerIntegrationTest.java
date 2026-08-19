package com.careerforge.controller;

import com.careerforge.dto.request.LoginRequest;
import com.careerforge.dto.request.UserStatusUpdateRequest;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.CompanyVerificationStatus;
import com.careerforge.entity.enums.Role;
import com.careerforge.entity.enums.SkillProficiency;
import com.careerforge.repository.*;
import com.careerforge.security.JwtTokenProvider;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminUserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private RecruiterProfileRepository recruiterProfileRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private StudentSkillRepository studentSkillRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private StorageService storageService;

    private User adminUser;
    private User studentUser;
    private User recruiterUser;

    private String adminToken;
    private String studentToken;
    private String recruiterToken;

    @BeforeEach
    void setUp() {
        when(storageService.store(any(MultipartFile.class)))
                .thenReturn("test_uuid.pdf");
        when(storageService.getFilePath(anyString()))
                .thenReturn(java.nio.file.Path.of("uploads/resumes/dummy.pdf"));
        when(storageService.loadAsResource(anyString()))
                .thenReturn(new ByteArrayResource("%PDF-1.4 Content".getBytes()));

        adminUser = userRepository.save(User.builder()
                .email("admin_int_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("AdminPass123!"))
                .role(Role.ROLE_ADMIN)
                .enabled(true)
                .build());

        studentUser = userRepository.save(User.builder()
                .email("student_int_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("StudentPass123!"))
                .role(Role.ROLE_STUDENT)
                .enabled(true)
                .build());

        recruiterUser = userRepository.save(User.builder()
                .email("recruiter_int_test@careerforge.local")
                .passwordHash(passwordEncoder.encode("RecruiterPass123!"))
                .role(Role.ROLE_RECRUITER)
                .enabled(true)
                .build());

        adminToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(adminUser));
        studentToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser));
        recruiterToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterUser));

        // Create student profile
        StudentProfile studentProfile = studentProfileRepository.save(StudentProfile.builder()
                .user(studentUser)
                .firstName("Alice")
                .lastName("Smith")
                .phone("+919876543210")
                .location("Bengaluru")
                .bio("Aspiring fullstack engineer")
                .educationSummary("B.Tech CSE")
                .profileCompletionPercentage(75)
                .build());

        Skill skill = skillRepository.save(Skill.builder().name("Java_AdminTest").category("Backend").build());
        studentSkillRepository.save(StudentSkill.builder()
                .studentProfile(studentProfile)
                .skill(skill)
                .proficiency(SkillProficiency.EXPERT)
                .build());

        // Create company & recruiter profile
        Company company = companyRepository.save(Company.builder()
                .name("Alpha Tech Innovations")
                .slug("alpha-tech-innovations")
                .industry("Software")
                .verificationStatus(CompanyVerificationStatus.VERIFIED)
                .build());

        recruiterProfileRepository.save(RecruiterProfile.builder()
                .user(recruiterUser)
                .company(company)
                .firstName("Bob")
                .lastName("Recruiter")
                .designation("Head of Talent")
                .phone("+919812345678")
                .isCompanyAdmin(true)
                .build());
    }

    // =========================================================================
    // 1. RBAC & Access Control Tests
    // =========================================================================

    @Test
    @DisplayName("Admin endpoint allows ROLE_ADMIN access")
    void testGetUsers_AdminAllowed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(3))));
    }

    @Test
    @DisplayName("Admin endpoint rejects ROLE_STUDENT with 403 Forbidden")
    void testGetUsers_StudentForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin endpoint rejects ROLE_RECRUITER with 403 Forbidden")
    void testGetUsers_RecruiterForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin endpoint rejects unauthenticated access with 401 Unauthorized")
    void testGetUsers_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 2. Search & Filtering Tests
    // =========================================================================

    @Test
    @DisplayName("Admin users list - filter by keyword name (matches student firstName)")
    void testGetUsers_SearchByFirstName() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users?search=Alice")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].email").value("student_int_test@careerforge.local"))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Alice Smith"));
    }

    @Test
    @DisplayName("Admin users list - filter by Role (ROLE_RECRUITER)")
    void testGetUsers_FilterByRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users?role=ROLE_RECRUITER")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].role").value("ROLE_RECRUITER"));
    }

    @Test
    @DisplayName("Admin users list - filter by enabled status")
    void testGetUsers_FilterByEnabled() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users?enabled=true")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(3))));
    }

    // =========================================================================
    // 3. User Detail Endpoint Tests
    // =========================================================================

    @Test
    @DisplayName("Admin user detail - returns complete sanitized student profile with skills")
    void testGetUserDetail_Student() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + studentUser.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("student_int_test@careerforge.local"))
                .andExpect(jsonPath("$.data.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.data.studentProfile").exists())
                .andExpect(jsonPath("$.data.studentProfile.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.studentProfile.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.studentProfile.totalSkills").value(1))
                .andExpect(jsonPath("$.data.studentProfile.skills[0]").value(containsString("Java_AdminTest")))
                .andExpect(jsonPath("$.data.recruiterProfile").doesNotExist());
    }

    @Test
    @DisplayName("Admin user detail - returns complete recruiter profile with company info")
    void testGetUserDetail_Recruiter() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/" + recruiterUser.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("recruiter_int_test@careerforge.local"))
                .andExpect(jsonPath("$.data.role").value("ROLE_RECRUITER"))
                .andExpect(jsonPath("$.data.recruiterProfile").exists())
                .andExpect(jsonPath("$.data.recruiterProfile.companyName").value("Alpha Tech Innovations"))
                .andExpect(jsonPath("$.data.recruiterProfile.isCompanyAdmin").value(true))
                .andExpect(jsonPath("$.data.studentProfile").doesNotExist());
    }

    @Test
    @DisplayName("Admin user detail - nonexistent user returns 404 Not Found")
    void testGetUserDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/999999")
                        .header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 4. User Status Modification & Self-Disable Protection Tests
    // =========================================================================

    @Test
    @DisplayName("Admin can disable and re-enable a student account")
    void testUpdateUserStatus_EnableDisable() throws Exception {
        // 1. Disable student account
        UserStatusUpdateRequest disableReq = UserStatusUpdateRequest.builder()
                .enabled(false)
                .reason("Terms violation review")
                .build();

        mockMvc.perform(patch("/api/v1/admin/users/" + studentUser.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        // 2. Re-enable student account
        UserStatusUpdateRequest enableReq = UserStatusUpdateRequest.builder()
                .enabled(true)
                .reason("Review cleared")
                .build();

        mockMvc.perform(patch("/api/v1/admin/users/" + studentUser.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(enableReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    @DisplayName("Admin attempting to disable own account is rejected with 400 Bad Request")
    void testUpdateUserStatus_SelfDisableRejected() throws Exception {
        UserStatusUpdateRequest selfDisableReq = UserStatusUpdateRequest.builder()
                .enabled(false)
                .reason("Accidental self disable")
                .build();

        mockMvc.perform(patch("/api/v1/admin/users/" + adminUser.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(selfDisableReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Administrators cannot disable their own account")));
    }

    // =========================================================================
    // 5. Disabled-User Security Enforcement Tests
    // =========================================================================

    @Test
    @DisplayName("Disabled user cannot log in (401 Unauthorized)")
    void testDisabledUser_CannotLogin() throws Exception {
        // Disable recruiter
        UserStatusUpdateRequest disableReq = UserStatusUpdateRequest.builder()
                .enabled(false)
                .reason("Account locked")
                .build();

        mockMvc.perform(patch("/api/v1/admin/users/" + recruiterUser.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableReq)))
                .andExpect(status().isOk());

        // Attempt login with valid credentials
        LoginRequest loginReq = LoginRequest.builder()
                .email("recruiter_int_test@careerforge.local")
                .password("RecruiterPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("User account is disabled")));
    }

    @Test
    @DisplayName("Previously issued JWT token is immediately rejected after account disablement (401 Unauthorized)")
    void testDisabledUser_PreviouslyIssuedTokenRejected() throws Exception {
        // Verify token works before disablement
        mockMvc.perform(get("/api/v1/students/profile")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());

        // Admin disables student account
        UserStatusUpdateRequest disableReq = UserStatusUpdateRequest.builder()
                .enabled(false)
                .reason("Account suspended")
                .build();

        mockMvc.perform(patch("/api/v1/admin/users/" + studentUser.getId() + "/status")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(disableReq)))
                .andExpect(status().isOk());

        // Use the SAME existing JWT token -> Must be rejected with 401 Unauthorized
        mockMvc.perform(get("/api/v1/students/profile")
                        .header("Authorization", studentToken))
                .andExpect(status().isUnauthorized());
    }
}
