package com.careerforge.integration;

import com.careerforge.dto.request.*;
import com.careerforge.dto.response.*;
import com.careerforge.entity.*;
import com.careerforge.entity.enums.*;
import com.careerforge.repository.*;
import com.careerforge.security.JwtTokenProvider;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.StorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
class Phase4EndToEndWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private StorageService storageService;

    private Skill javaSkill;
    private Skill springBootSkill;
    private Skill reactSkill;
    private Skill mysqlSkill;

    @BeforeEach
    void setUp() {
        // Seed deterministic skills
        javaSkill = getOrCreateSkill("Java", "Backend");
        springBootSkill = getOrCreateSkill("Spring Boot", "Backend");
        reactSkill = getOrCreateSkill("React", "Frontend");
        mysqlSkill = getOrCreateSkill("MySQL", "Database");

        // Mock StorageService globally
        when(storageService.store(any(MultipartFile.class)))
                .thenAnswer(i -> "test_uuid_" + System.nanoTime() + ".pdf");
        when(storageService.getFilePath(anyString()))
                .thenAnswer(i -> java.nio.file.Path.of("uploads/resumes/" + i.getArgument(0)));
        when(storageService.loadAsResource(anyString()))
                .thenReturn(new ByteArrayResource("%PDF-1.4 Minimal PDF Content".getBytes()));
    }

    private Skill getOrCreateSkill(String name, String category) {
        return skillRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> skillRepository.save(Skill.builder().name(name).category(category).build()));
    }

    // =========================================================================
    // Test 1: Complete End-to-End Recruitment Lifecycle & Student Journey
    // =========================================================================

    @Test
    @DisplayName("Complete E2E Scenario: Student Profile -> Recruiter Job -> Matching -> Application -> ATS Pipeline -> Notifications -> Saved Jobs")
    void test1_CompleteRecruitmentWorkflow_StudentAndRecruiterJourney() throws Exception {
        // ---------------------------------------------------------------------
        // 1. Student Registration & Login
        // ---------------------------------------------------------------------
        RegisterRequest studentReg = RegisterRequest.builder()
                .email("student_e2e_user@careerforge.local")
                .password("DevPass123!")
                .role(Role.ROLE_STUDENT)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentReg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.user.role").value("ROLE_STUDENT"));

        LoginRequest studentLogin = LoginRequest.builder()
                .email("student_e2e_user@careerforge.local")
                .password("DevPass123!")
                .build();

        MvcResult studentLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(studentLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String studentToken = "Bearer " + extractAccessToken(studentLoginRes);

        // ---------------------------------------------------------------------
        // 2. Student Profile Creation (Completion >= 30%)
        // ---------------------------------------------------------------------
        StudentProfileRequest profileReq = StudentProfileRequest.builder()
                .firstName("Alice")
                .lastName("Smith")
                .location("Bengaluru, India")
                .phone("+919876543210")
                .bio("Aspiring full-stack engineer passionate about cloud-native systems.")
                .educationSummary("B.Tech in Computer Science and Engineering")
                .githubUrl("https://github.com/alicesmith")
                .linkedinUrl("https://linkedin.com/in/alicesmith")
                .build();

        mockMvc.perform(put("/api/v1/students/profile")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profileReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.profileCompletionPercentage", greaterThanOrEqualTo(30)));

        // ---------------------------------------------------------------------
        // 3. Add Student Skills (Java: ADVANCED = 3)
        // ---------------------------------------------------------------------
        StudentSkillRequest skillReq = StudentSkillRequest.builder()
                .skillId(javaSkill.getId())
                .proficiency(SkillProficiency.ADVANCED)
                .build();

        mockMvc.perform(post("/api/v1/students/skills")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.skillName").value("Java"))
                .andExpect(jsonPath("$.data.proficiency").value("ADVANCED"));

        // ---------------------------------------------------------------------
        // 4. Upload & Activate Resume
        // ---------------------------------------------------------------------
        MockMultipartFile resumeFile = new MockMultipartFile(
                "file",
                "Alice_Resume_2026.pdf",
                "application/pdf",
                "%PDF-1.4 Minimal PDF Content".getBytes()
        );

        MvcResult resumeRes = mockMvc.perform(multipart("/api/v1/students/resumes")
                        .file(resumeFile)
                        .header("Authorization", studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.originalFileName").value("Alice_Resume_2026.pdf"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        Long resumeId = objectMapper.readTree(resumeRes.getResponse().getContentAsString()).get("data").get("id").asLong();

        // ---------------------------------------------------------------------
        // 5. Recruiter Registration & Login
        // ---------------------------------------------------------------------
        RegisterRequest recruiterReg = RegisterRequest.builder()
                .email("recruiter_e2e_alpha@careerforge.local")
                .password("DevPass123!")
                .role(Role.ROLE_RECRUITER)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recruiterReg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.accessToken").exists());

        LoginRequest recruiterLogin = LoginRequest.builder()
                .email("recruiter_e2e_alpha@careerforge.local")
                .password("DevPass123!")
                .build();

        MvcResult recruiterLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recruiterLogin)))
                .andExpect(status().isOk())
                .andReturn();

        String recruiterToken = "Bearer " + extractAccessToken(recruiterLoginRes);

        // ---------------------------------------------------------------------
        // 6. Recruiter Profile Creation
        // ---------------------------------------------------------------------
        RecruiterProfileRequest recProfileReq = RecruiterProfileRequest.builder()
                .firstName("Bob")
                .lastName("Talent")
                .designation("Head of Technical Recruiting")
                .phone("+919812345678")
                .department("Talent Acquisition")
                .build();

        mockMvc.perform(put("/api/v1/recruiters/profile")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recProfileReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Bob"));

        // ---------------------------------------------------------------------
        // 7. Register Hiring Company
        // ---------------------------------------------------------------------
        CompanyCreateRequest companyReq = CompanyCreateRequest.builder()
                .name("Apex Cloud Innovations")
                .industry("Cloud Software & Infrastructure")
                .website("https://apexcloud.example.com")
                .location("Bengaluru, India")
                .description("Next generation cloud orchestration platforms.")
                .companySize("50-200 employees")
                .build();

        MvcResult companyRes = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Apex Cloud Innovations"))
                .andReturn();

        Long companyId = objectMapper.readTree(companyRes.getResponse().getContentAsString()).get("data").get("id").asLong();
        companyRepository.findById(companyId).ifPresent(c -> {
            c.setVerificationStatus(com.careerforge.entity.enums.CompanyVerificationStatus.VERIFIED);
            companyRepository.save(c);
        });

        // ---------------------------------------------------------------------
        // 8. Create Job Posting as DRAFT
        // ---------------------------------------------------------------------
        JobCreateRequest jobReq = JobCreateRequest.builder()
                .title("Senior Cloud Backend Engineer")
                .description("Design and scale distributed microservices in Java.")
                .location("Bengaluru, India")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(BigDecimal.valueOf(1800000))
                .salaryMax(BigDecimal.valueOf(2800000))
                .currency("INR")
                .deadline(LocalDateTime.now().plusDays(45))
                .skills(List.of(
                        JobSkillItemRequest.builder()
                                .skillId(javaSkill.getId())
                                .isRequired(true)
                                .minimumProficiency(SkillProficiency.ADVANCED)
                                .build()
                ))
                .build();

        MvcResult jobRes = mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        Long jobId = objectMapper.readTree(jobRes.getResponse().getContentAsString()).get("data").get("id").asLong();

        // ---------------------------------------------------------------------
        // 10. Publish Job Posting
        // ---------------------------------------------------------------------
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/publish")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // ---------------------------------------------------------------------
        // 11. Public Job Discovery
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/jobs?search=Cloud&location=Bengaluru")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].title").value("Senior Cloud Backend Engineer"));

        // ---------------------------------------------------------------------
        // 12 & 13. Student Match Preview & Deterministic Score Verification (100.00%)
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/students/jobs/" + jobId + "/match-preview")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallScore").value(100.00))
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.totalRequiredCount").value(1))
                .andExpect(jsonPath("$.data.matchedRequiredCount").value(1));

        // ---------------------------------------------------------------------
        // 14, 15, 16. Student Application Submission (Resolving Active Resume & Snapshot Score)
        // ---------------------------------------------------------------------
        ApplicationSubmitRequest appSubmit = ApplicationSubmitRequest.builder()
                .jobId(jobId)
                .coverLetter("Excited about building cloud orchestration systems at Apex Cloud Innovations.")
                .build(); // resumeId omitted to trigger active resume fallback

        MvcResult appRes = mockMvc.perform(post("/api/v1/students/applications")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appSubmit)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.matchScoreAtApplication").value(100.00))
                .andExpect(jsonPath("$.data.resumeId").value(resumeId))
                .andExpect(jsonPath("$.data.jobTitle").value("Senior Cloud Backend Engineer"))
                .andReturn();

        Long applicationId = objectMapper.readTree(appRes.getResponse().getContentAsString()).get("data").get("id").asLong();

        // ---------------------------------------------------------------------
        // 17. Duplicate Application Rejection (400 Bad Request)
        // ---------------------------------------------------------------------
        mockMvc.perform(post("/api/v1/students/applications")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appSubmit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already submitted")));

        // ---------------------------------------------------------------------
        // 18. Student Lists Own Applications
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/students/applications")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(applicationId))
                .andExpect(jsonPath("$.data.content[0].matchScoreAtApplication").value(100.00));

        // ---------------------------------------------------------------------
        // 19 & 20. Recruiter ATS: Applicant Listing (Company Scoped & Score Sorted)
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/recruiters/jobs/" + jobId + "/applications?sortBy=matchScoreAtApplication&sortDirection=desc")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].candidateName").value("Alice Smith"))
                .andExpect(jsonPath("$.data.content[0].matchScoreAtApplication").value(100.00))
                .andExpect(jsonPath("$.data.content[0].status").value("APPLIED"));

        // ---------------------------------------------------------------------
        // 21. Recruiter ATS: APPLIED -> UNDER_REVIEW (Records reviewedAt)
        // ---------------------------------------------------------------------
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.UNDER_REVIEW)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data.reviewedAt").exists());

        // ---------------------------------------------------------------------
        // 22. Recruiter Adds Evaluation Notes
        // ---------------------------------------------------------------------
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/notes")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationNotesRequest.builder()
                                .recruiterNotes("Strong Java background and active GitHub profile. Fast-track to interview.")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recruiterNotes").value("Strong Java background and active GitHub profile. Fast-track to interview."));

        // ---------------------------------------------------------------------
        // 23. Recruiter ATS: UNDER_REVIEW -> SHORTLISTED
        // ---------------------------------------------------------------------
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.SHORTLISTED)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));

        // ---------------------------------------------------------------------
        // 24. Recruiter ATS: SHORTLISTED -> INTERVIEW_SCHEDULED (Future Timestamp)
        // ---------------------------------------------------------------------
        LocalDateTime interviewDate1 = LocalDateTime.now().plusDays(3);
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.INTERVIEW_SCHEDULED)
                                .interviewScheduledAt(interviewDate1)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data.interviewScheduledAt").exists());

        // ---------------------------------------------------------------------
        // 25. Recruiter ATS: Reschedule Interview (INTERVIEW_SCHEDULED -> INTERVIEW_SCHEDULED)
        // ---------------------------------------------------------------------
        LocalDateTime interviewDate2 = LocalDateTime.now().plusDays(5);
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.INTERVIEW_SCHEDULED)
                                .interviewScheduledAt(interviewDate2)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INTERVIEW_SCHEDULED"));

        // ---------------------------------------------------------------------
        // 26. Recruiter ATS: INTERVIEW_SCHEDULED -> ACCEPTED (Offer extended)
        // ---------------------------------------------------------------------
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.ACCEPTED)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // ---------------------------------------------------------------------
        // 27. Terminal State Immutability (ACCEPTED -> REJECTED Rejected with 400)
        // ---------------------------------------------------------------------
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.REJECTED)
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("terminal")));

        // ---------------------------------------------------------------------
        // 28, 29, 30, 31, 32, 33. Notification Lifecycle for Candidate
        // ---------------------------------------------------------------------
        // Verify multiple dispatched notifications
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(3))));

        // Verify unread count > 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount", greaterThan(0)));

        // Mark all as read
        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());

        // Verify unread count is now 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        // ---------------------------------------------------------------------
        // 34, 35, 36, 37. Saved Jobs Ecosystem
        // ---------------------------------------------------------------------
        // Save published job
        mockMvc.perform(post("/api/v1/students/saved-jobs/" + jobId)
                        .header("Authorization", studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.jobId").value(jobId));

        // List saved jobs
        mockMvc.perform(get("/api/v1/students/saved-jobs")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].jobId").value(jobId));

        // Duplicate save rejection (400)
        mockMvc.perform(post("/api/v1/students/saved-jobs/" + jobId)
                        .header("Authorization", studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already saved")));

        // Remove saved job
        mockMvc.perform(delete("/api/v1/students/saved-jobs/" + jobId)
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());

        // Verify saved job list is now empty
        mockMvc.perform(get("/api/v1/students/saved-jobs")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // =========================================================================
    // Test 2: Cross-Tenant, Cross-Company, and Cross-Student Security Isolation
    // =========================================================================

    @Test
    @DisplayName("Security & Isolation: Recruiter B vs Company A (404), Student B vs Student A (404), Cross-Role RBAC (403)")
    void test2_SecurityAndCrossTenantIsolation() throws Exception {
        // Setup Recruiter A + Company A + Job A + Student A Application
        String tokenRecruiterA = registerAndGetToken("rec_alpha_sec@careerforge.local", Role.ROLE_RECRUITER);
        String tokenRecruiterB = registerAndGetToken("rec_beta_sec@careerforge.local", Role.ROLE_RECRUITER);
        String tokenStudentA = registerAndGetToken("student_alpha_sec@careerforge.local", Role.ROLE_STUDENT);
        String tokenStudentB = registerAndGetToken("student_beta_sec@careerforge.local", Role.ROLE_STUDENT);

        setupRecruiterProfile(tokenRecruiterA, "Alice", "Alpha");
        setupRecruiterProfile(tokenRecruiterB, "Bob", "Beta");
        setupStudentProfile(tokenStudentA, "Student", "Alpha");
        setupStudentProfile(tokenStudentB, "Student", "Beta");

        Long companyAId = createCompany(tokenRecruiterA, "Company Alpha Sec");
        Long companyBId = createCompany(tokenRecruiterB, "Company Beta Sec");

        Long jobAId = createAndPublishJob(tokenRecruiterA, "Backend Engineer Alpha");

        // Student A applies to Job A
        MvcResult appResult = mockMvc.perform(post("/api/v1/students/applications")
                        .header("Authorization", tokenStudentA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationSubmitRequest.builder().jobId(jobAId).build())))
                .andExpect(status().isCreated())
                .andReturn();

        Long appAId = objectMapper.readTree(appResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // ---------------------------------------------------------------------
        // 38. Recruiter B cannot access Company A's job details -> 404
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/recruiters/jobs/" + jobAId)
                        .header("Authorization", tokenRecruiterB))
                .andExpect(status().isNotFound());

        // ---------------------------------------------------------------------
        // 39. Recruiter B cannot access Company A's applicant dossier -> 404
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/recruiters/applications/" + appAId)
                        .header("Authorization", tokenRecruiterB))
                .andExpect(status().isNotFound());

        // Recruiter B cannot update status of Company A's application -> 404
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appAId + "/status")
                        .header("Authorization", tokenRecruiterB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.REJECTED)
                                .build())))
                .andExpect(status().isNotFound());

        // ---------------------------------------------------------------------
        // 40. Student B cannot access Student A's application details -> 404
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/students/applications/" + appAId)
                        .header("Authorization", tokenStudentB))
                .andExpect(status().isNotFound());

        // Student B cannot withdraw Student A's application -> 404
        mockMvc.perform(patch("/api/v1/students/applications/" + appAId + "/withdraw")
                        .header("Authorization", tokenStudentB))
                .andExpect(status().isNotFound());

        // ---------------------------------------------------------------------
        // 41. Student attempting Recruiter endpoint -> 403 Forbidden
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/recruiters/jobs/" + jobAId + "/applications")
                        .header("Authorization", tokenStudentA))
                .andExpect(status().isForbidden());

        // ---------------------------------------------------------------------
        // 42. Recruiter attempting Student application endpoint -> 403 Forbidden
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/students/applications")
                        .header("Authorization", tokenRecruiterA))
                .andExpect(status().isForbidden());

        // ---------------------------------------------------------------------
        // 43. Recruiter B attempting to download Company A applicant resume -> 404
        // ---------------------------------------------------------------------
        mockMvc.perform(get("/api/v1/recruiters/applications/" + appAId + "/resume/download")
                        .header("Authorization", tokenRecruiterB))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // Test 3: Job Lifecycle Transitions & Public Discovery Guard
    // =========================================================================

    @Test
    @DisplayName("Job Lifecycle: PUBLISHED -> DRAFT -> CLOSED -> REOPENED, with public visibility & application guards")
    void test3_JobLifecycleAndPublicDiscovery() throws Exception {
        String tokenRecruiter = registerAndGetToken("rec_lifecycle@careerforge.local", Role.ROLE_RECRUITER);
        String tokenStudent = registerAndGetToken("student_lifecycle@careerforge.local", Role.ROLE_STUDENT);

        setupRecruiterProfile(tokenRecruiter, "John", "Lifecycle");
        setupStudentProfile(tokenStudent, "Sara", "Lifecycle");
        createCompany(tokenRecruiter, "Lifecycle Technologies");

        // 1. Create Job as DRAFT
        JobCreateRequest jobReq = JobCreateRequest.builder()
                .title("Distributed Systems Architect")
                .description("Build high performance event pipelines.")
                .location("Hyderabad, India")
                .workMode(WorkMode.REMOTE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .salaryMin(BigDecimal.valueOf(3000000))
                .salaryMax(BigDecimal.valueOf(4500000))
                .deadline(LocalDateTime.now().plusDays(60))
                .skills(List.of(
                        JobSkillItemRequest.builder().skillId(javaSkill.getId()).isRequired(true).build()
                ))
                .build();

        MvcResult createRes = mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", tokenRecruiter)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        Long jobId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 2. Draft job must NOT be discoverable publicly
        mockMvc.perform(get("/api/v1/jobs?search=Distributed")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // 3. Publish Job
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/publish")
                        .header("Authorization", tokenRecruiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 4. Published job MUST be discoverable publicly
        mockMvc.perform(get("/api/v1/jobs?search=Distributed")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(jobId));

        // 5. Unpublish Job (PUBLISHED -> DRAFT)
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/unpublish")
                        .header("Authorization", tokenRecruiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // 6. DRAFT job cannot receive applications
        mockMvc.perform(post("/api/v1/students/applications")
                        .header("Authorization", tokenStudent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationSubmitRequest.builder().jobId(jobId).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("unpublished")));

        // 7. Re-publish and Close Job (PUBLISHED -> CLOSED)
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/publish")
                        .header("Authorization", tokenRecruiter))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/close")
                        .header("Authorization", tokenRecruiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // 8. CLOSED job cannot receive applications
        mockMvc.perform(post("/api/v1/students/applications")
                        .header("Authorization", tokenStudent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationSubmitRequest.builder().jobId(jobId).build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("unpublished")));

        // 9. Reopen Job (CLOSED -> PUBLISHED)
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/reopen")
                        .header("Authorization", tokenRecruiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 10. Reopened job accepts applications
        mockMvc.perform(post("/api/v1/students/applications")
                        .header("Authorization", tokenStudent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationSubmitRequest.builder().jobId(jobId).build())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    // =========================================================================
    // Test 4: Recruiter Applicant Multi-Criteria Filtering & Notes Persistence
    // =========================================================================

    @Test
    @DisplayName("Recruiter ATS: Multi-Criteria Filtering (status, minScore, maxScore, candidate search)")
    void test4_RecruiterApplicantFilteringAndNotes() throws Exception {
        String tokenRecruiter = registerAndGetToken("rec_filter@careerforge.local", Role.ROLE_RECRUITER);
        String tokenStudent = registerAndGetToken("student_filter@careerforge.local", Role.ROLE_STUDENT);

        setupRecruiterProfile(tokenRecruiter, "Filter", "Recruiter");
        setupStudentProfile(tokenStudent, "Emma", "Watson");
        createCompany(tokenRecruiter, "Filter Solutions");

        Long jobId = createAndPublishJob(tokenRecruiter, "Senior Microservices Dev");

        // Emma applies
        mockMvc.perform(post("/api/v1/students/applications")
                        .header("Authorization", tokenStudent)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationSubmitRequest.builder().jobId(jobId).build())))
                .andExpect(status().isCreated());

        // Filter by Candidate Search: "Emma"
        mockMvc.perform(get("/api/v1/recruiters/jobs/" + jobId + "/applications?search=Emma")
                        .header("Authorization", tokenRecruiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].candidateName").value("Emma Watson"));

        // Filter by Candidate Search: "NonExistent"
        mockMvc.perform(get("/api/v1/recruiters/jobs/" + jobId + "/applications?search=NonExistent")
                        .header("Authorization", tokenRecruiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // Filter by Status: "APPLIED"
        mockMvc.perform(get("/api/v1/recruiters/jobs/" + jobId + "/applications?status=APPLIED")
                        .header("Authorization", tokenRecruiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // Filter by Status: "SHORTLISTED"
        mockMvc.perform(get("/api/v1/recruiters/jobs/" + jobId + "/applications?status=SHORTLISTED")
                        .header("Authorization", tokenRecruiter))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    // =========================================================================
    // Helper Methods
    // =========================================================================

    private String registerAndGetToken(String email, Role role) throws Exception {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.builder()
                        .email(email)
                        .passwordHash(passwordEncoder.encode("DevPass123!"))
                        .role(role)
                        .enabled(true)
                        .build()));

        return "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(user));
    }

    private void setupStudentProfile(String token, String firstName, String lastName) throws Exception {
        StudentProfileRequest req = StudentProfileRequest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .location("Bengaluru, India")
                .phone("+919876543210")
                .bio("Software engineering student.")
                .educationSummary("B.Tech CSE")
                .build();

        mockMvc.perform(put("/api/v1/students/profile")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Add skill for student profile completion >= 30%
        StudentSkillRequest skillReq = StudentSkillRequest.builder()
                .skillId(javaSkill.getId())
                .proficiency(SkillProficiency.ADVANCED)
                .build();

        mockMvc.perform(post("/api/v1/students/skills")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillReq)))
                .andExpect(status().isCreated());

        // Upload resume
        MockMultipartFile resumeFile = new MockMultipartFile(
                "file",
                firstName + "_Resume.pdf",
                "application/pdf",
                "%PDF-1.4 Dummy PDF".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/students/resumes")
                        .file(resumeFile)
                        .header("Authorization", token))
                .andExpect(status().isCreated());
    }

    private void setupRecruiterProfile(String token, String firstName, String lastName) throws Exception {
        RecruiterProfileRequest req = RecruiterProfileRequest.builder()
                .firstName(firstName)
                .lastName(lastName)
                .designation("Technical Recruiter")
                .phone("+919812345678")
                .department("Talent Acquisition")
                .build();

        mockMvc.perform(put("/api/v1/recruiters/profile")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private Long createCompany(String token, String companyName) throws Exception {
        CompanyCreateRequest companyReq = CompanyCreateRequest.builder()
                .name(companyName)
                .industry("Information Technology")
                .website("https://example.com")
                .location("Bengaluru")
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(companyReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long companyId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();
        companyRepository.findById(companyId).ifPresent(c -> {
            c.setVerificationStatus(com.careerforge.entity.enums.CompanyVerificationStatus.VERIFIED);
            companyRepository.save(c);
        });
        return companyId;
    }

    private Long createAndPublishJob(String token, String title) throws Exception {
        JobCreateRequest jobReq = JobCreateRequest.builder()
                .title(title)
                .description("Core platform development.")
                .location("Bengaluru")
                .workMode(WorkMode.REMOTE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(BigDecimal.valueOf(1500000))
                .salaryMax(BigDecimal.valueOf(2200000))
                .deadline(LocalDateTime.now().plusDays(30))
                .skills(List.of(
                        JobSkillItemRequest.builder().skillId(javaSkill.getId()).isRequired(true).build()
                ))
                .build();

        MvcResult res = mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long jobId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/publish")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        return jobId;
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data").get("accessToken").asText();
    }
}
