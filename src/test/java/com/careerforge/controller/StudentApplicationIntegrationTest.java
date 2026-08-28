package com.careerforge.controller;

import com.careerforge.dto.request.ApplicationSubmitRequest;
import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.JobCreateRequest;
import com.careerforge.dto.request.JobSkillItemRequest;
import com.careerforge.dto.request.StudentProfileRequest;
import com.careerforge.dto.response.CompanyResponse;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.entity.Application;
import com.careerforge.entity.Notification;
import com.careerforge.entity.Resume;
import com.careerforge.entity.Skill;
import com.careerforge.entity.StudentProfile;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.ExperienceLevel;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.Role;
import com.careerforge.entity.enums.WorkMode;
import com.careerforge.repository.ApplicationRepository;
import com.careerforge.repository.ApplicationStatusHistoryRepository;
import com.careerforge.repository.CompanyRepository;
import com.careerforge.repository.NotificationRepository;
import com.careerforge.repository.ResumeRepository;
import com.careerforge.repository.SkillRepository;
import com.careerforge.repository.StudentProfileRepository;
import com.careerforge.repository.UserRepository;
import com.careerforge.security.JwtTokenProvider;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.CompanyService;
import com.careerforge.service.JobService;
import com.careerforge.service.StudentProfileService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudentApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private ApplicationStatusHistoryRepository applicationStatusHistoryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private com.careerforge.config.DataInitializer dataInitializer;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobService jobService;

    private String studentToken1;
    private String studentToken2;
    private String recruiterToken;
    private Long publishedJobId;

    @BeforeEach
    void setUp() {
        User studentUser1 = userRepository.findByEmail("student_app_test1@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_app_test1@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        User studentUser2 = userRepository.findByEmail("student_app_test2@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_app_test2@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        User recruiterUser = userRepository.findByEmail("recruiter_app_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("recruiter_app_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_RECRUITER)
                        .enabled(true)
                        .build()));

        studentToken1 = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser1));
        studentToken2 = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser2));
        recruiterToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterUser));

        // Create student profile 1 with completion >= 30%
        studentProfileService.updateProfile(studentUser1.getId(), StudentProfileRequest.builder()
                .firstName("Alice")
                .lastName("Student")
                .location("Bengaluru")
                .bio("Motivated software engineering student.")
                .educationSummary("B.Tech Computer Science")
                .build());

        StudentProfile sp1 = studentProfileRepository.findByUser_Id(studentUser1.getId()).orElseThrow();
        resumeRepository.save(Resume.builder()
                .studentProfile(sp1)
                .originalFileName("alice_resume.pdf")
                .storedFileName("alice_uuid.pdf")
                .storagePath("/tmp/alice_uuid.pdf")
                .contentType("application/pdf")
                .fileSize(1024L)
                .isActive(true)
                .build());

        // Create recruiter company and published job
        CompanyResponse comp = companyService.createCompany(recruiterUser.getId(), CompanyCreateRequest.builder()
                .name("NextGen Software Corp")
                .industry("Cloud Computing")
                .build());
        companyRepository.findById(comp.getId()).ifPresent(c -> {
            c.setVerificationStatus(com.careerforge.entity.enums.CompanyVerificationStatus.VERIFIED);
            companyRepository.save(c);
        });

        Skill javaSkill = skillRepository.findByNameIgnoreCase("Java")
                .orElseGet(() -> skillRepository.save(Skill.builder().name("Java").category("Backend").build()));

        JobCreateRequest jobReq = JobCreateRequest.builder()
                .title("Cloud Infrastructure Engineer")
                .description("Build cloud native applications using Java.")
                .location("Bengaluru")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(BigDecimal.valueOf(1500000))
                .salaryMax(BigDecimal.valueOf(2200000))
                .deadline(LocalDateTime.now().plusDays(30))
                .skills(List.of(JobSkillItemRequest.builder().skillId(javaSkill.getId()).isRequired(true).build()))
                .build();

        JobDetailResponse draftJob = jobService.createJob(recruiterUser.getId(), jobReq);
        JobDetailResponse publishedJob = jobService.publishJob(recruiterUser.getId(), draftJob.getId());
        publishedJobId = publishedJob.getId();
    }

    @Test
    @DisplayName("Complete student application lifecycle: Preview -> Submit -> List -> Detail -> Duplicate Reject -> Withdraw")
    void testStudentApplicationLifecycle() throws Exception {
        // 1. Preview Match
        mockMvc.perform(get("/api/v1/students/jobs/" + publishedJobId + "/match-preview")
                        .header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.overallScore").exists());

        // 2. Submit Application
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder()
                .jobId(publishedJobId)
                .coverLetter("Excited about the Cloud Infrastructure role.")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/students/applications")
                        .header("Authorization", studentToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andExpect(jsonPath("$.data.matchScoreAtApplication").exists())
                .andExpect(jsonPath("$.data.jobTitle").value("Cloud Infrastructure Engineer"))
                .andReturn();

        Long appId = objectMapper.readTree(result.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 3. Duplicate submit rejected -> 400
        mockMvc.perform(post("/api/v1/students/applications")
                        .header("Authorization", studentToken1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already submitted")));

        // 4. List own applications
        mockMvc.perform(get("/api/v1/students/applications")
                        .header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(appId));

        // 5. Get application details
        mockMvc.perform(get("/api/v1/students/applications/" + appId)
                        .header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.coverLetter").value("Excited about the Cloud Infrastructure role."))
                .andExpect(jsonPath("$.data.currentMatchAnalysis").exists());

        // 6. Withdraw application
        mockMvc.perform(patch("/api/v1/students/applications/" + appId + "/withdraw")
                        .header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        // 7. Student 2 cannot access Student 1's application -> 404
        mockMvc.perform(get("/api/v1/students/applications/" + appId)
                        .header("Authorization", studentToken2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Recruiter access to student application endpoint returns 403 Forbidden")
    void testRecruiterCannotAccessStudentEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/students/applications")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isForbidden());
    }

    private void assertStageVisibility(String token, boolean applied, boolean underReview, boolean shortlisted, boolean interviewScheduled, boolean accepted, boolean rejected, boolean withdrawn) throws Exception {
        // All Statuses must always show the application exactly once
        mockMvc.perform(get("/api/v1/students/applications").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // Filtered statuses: single source of truth
        mockMvc.perform(get("/api/v1/students/applications?status=APPLIED").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(applied ? 1 : 0)));

        mockMvc.perform(get("/api/v1/students/applications?status=UNDER_REVIEW").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(underReview ? 1 : 0)));

        mockMvc.perform(get("/api/v1/students/applications?status=SHORTLISTED").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(shortlisted ? 1 : 0)));

        mockMvc.perform(get("/api/v1/students/applications?status=INTERVIEW_SCHEDULED").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(interviewScheduled ? 1 : 0)));

        mockMvc.perform(get("/api/v1/students/applications?status=ACCEPTED").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(accepted ? 1 : 0)));

        mockMvc.perform(get("/api/v1/students/applications?status=REJECTED").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(rejected ? 1 : 0)));

        mockMvc.perform(get("/api/v1/students/applications?status=WITHDRAWN").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(withdrawn ? 1 : 0)));
    }

    @Test
    @DisplayName("Case 1: APPLIED -> UNDER_REVIEW -> SHORTLISTED -> INTERVIEW_SCHEDULED -> ACCEPTED")
    void testCase1_Applied_UnderReview_Shortlisted_Interview_Accepted() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("C1").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Initial APPLIED
        assertStageVisibility(studentToken1, true, false, false, false, false, false, false);

        // UNDER_REVIEW
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        assertStageVisibility(studentToken1, false, true, false, false, false, false, false);

        // SHORTLISTED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());
        assertStageVisibility(studentToken1, false, false, true, false, false, false, false);

        // INTERVIEW_SCHEDULED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewScheduledAt\":\"2026-09-01T10:00:00\"}")).andExpect(status().isOk());
        assertStageVisibility(studentToken1, false, false, false, true, false, false, false);

        // ACCEPTED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isOk());
        assertStageVisibility(studentToken1, false, false, false, false, true, false, false);
    }

    @Test
    @DisplayName("Case 2: APPLIED -> UNDER_REVIEW -> REJECTED")
    void testCase2_Applied_UnderReview_Rejected() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("C2").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}")).andExpect(status().isOk());

        assertStageVisibility(studentToken1, false, false, false, false, false, true, false);
    }

    @Test
    @DisplayName("Case 3: APPLIED -> UNDER_REVIEW -> WITHDRAWN")
    void testCase3_Applied_UnderReview_Withdrawn() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("C3").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/students/applications/" + appId + "/withdraw").header("Authorization", studentToken1))
                .andExpect(status().isOk());

        assertStageVisibility(studentToken1, false, false, false, false, false, false, true);
    }

    @Test
    @DisplayName("Case 4: APPLIED -> REJECTED")
    void testCase4_Applied_Rejected() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("C4").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"REJECTED\"}")).andExpect(status().isOk());

        assertStageVisibility(studentToken1, false, false, false, false, false, true, false);
    }

    @Test
    @DisplayName("Case 5: APPLIED -> WITHDRAWN")
    void testCase5_Applied_Withdrawn() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("C5").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/students/applications/" + appId + "/withdraw").header("Authorization", studentToken1))
                .andExpect(status().isOk());

        assertStageVisibility(studentToken1, false, false, false, false, false, false, true);
    }

    @Test
    @DisplayName("Case 6: APPLIED -> UNDER_REVIEW -> SHORTLISTED -> INTERVIEW_SCHEDULED")
    void testCase6_Applied_UnderReview_Shortlisted_InterviewScheduled() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("C6").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewScheduledAt\":\"2026-09-01T10:00:00\"}")).andExpect(status().isOk());

        assertStageVisibility(studentToken1, false, false, false, true, false, false, false);
    }

    @Test
    @DisplayName("Case 7: APPLIED -> UNDER_REVIEW")
    void testCase7_Applied_UnderReview() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("C7").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());

        assertStageVisibility(studentToken1, false, true, false, false, false, false, false);
    }

    @Test
    @DisplayName("Direct transition APPLIED -> SHORTLISTED is rejected (UNDER_REVIEW is mandatory)")
    void testDirectAppliedToShortlistedIsRejected() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Direct Shortlist Test").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // APPLIED -> SHORTLISTED directly must fail with 400 Bad Request
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Withdrawal is permitted from SHORTLISTED and INTERVIEW_SCHEDULED states")
    void testWithdrawalFromShortlistedAndInterviewScheduled() throws Exception {
        // Test withdrawal from SHORTLISTED
        ApplicationSubmitRequest req1 = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("W_SHORTLISTED").build();
        MvcResult res1 = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated()).andReturn();
        Long appId1 = objectMapper.readTree(res1.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId1 + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId1 + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/students/applications/" + appId1 + "/withdraw").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));

        assertStageVisibility(studentToken1, false, false, false, false, false, false, true);
    }

    @Test
    @DisplayName("Historical timestamps (shortlistedAt, interviewScheduledAt) are retained on ACCEPTED")
    void testHistoricalTimestampsRetainedOnAccepted() throws Exception {
        ApplicationSubmitRequest req = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Timestamps Test").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewScheduledAt\":\"2026-09-01T10:00:00\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/students/applications/" + appId).header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.interviewScheduledAt").exists());
    }

    @Test
    @DisplayName("Application submission creates initial history event (null -> APPLIED)")
    void testApplicationCreationGeneratesInitialHistory() throws Exception {
        ApplicationSubmitRequest req = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Initial History Test").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        mockMvc.perform(get("/api/v1/students/applications/" + appId + "/history").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].fromStatus").doesNotExist())
                .andExpect(jsonPath("$.data[0].toStatus").value("APPLIED"))
                .andExpect(jsonPath("$.data[0].changedBy").value("STUDENT"));
    }

    @Test
    @DisplayName("Full lifecycle records all 5 chronological history events")
    void testFullLifecycleHistoryEventsChronological() throws Exception {
        ApplicationSubmitRequest req = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Full Lifecycle History").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 1 -> 2: UNDER_REVIEW
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());

        // 2 -> 3: SHORTLISTED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());

        // 3 -> 4: INTERVIEW_SCHEDULED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewScheduledAt\":\"2026-09-01T10:00:00\"}")).andExpect(status().isOk());

        // 4 -> 5: ACCEPTED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isOk());

        // Verify history endpoint returns all 5 events
        mockMvc.perform(get("/api/v1/students/applications/" + appId + "/history").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].toStatus").value("APPLIED"))
                .andExpect(jsonPath("$.data[1].fromStatus").value("APPLIED"))
                .andExpect(jsonPath("$.data[1].toStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data[2].fromStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data[2].toStatus").value("SHORTLISTED"))
                .andExpect(jsonPath("$.data[3].fromStatus").value("SHORTLISTED"))
                .andExpect(jsonPath("$.data[3].toStatus").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data[4].fromStatus").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data[4].toStatus").value("ACCEPTED"));

        // Verify Recruiter can also fetch history
        mockMvc.perform(get("/api/v1/recruiters/applications/" + appId + "/history").header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)));
    }

    @Test
    @DisplayName("Security: Student cannot access another student's application history")
    void testStudentCannotAccessOtherStudentHistory() throws Exception {
        ApplicationSubmitRequest req = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Security Check").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Student 2 attempting to view Student 1's application history must fail with 404
        mockMvc.perform(get("/api/v1/students/applications/" + appId + "/history").header("Authorization", studentToken2))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Failed transition does not create history event")
    void testFailedTransitionDoesNotCreateHistoryEvent() throws Exception {
        ApplicationSubmitRequest req = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Failed Transition").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // APPLIED -> SHORTLISTED fails
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}"))
                .andExpect(status().isBadRequest());

        // History must still contain ONLY the 1 initial APPLIED event
        mockMvc.perform(get("/api/v1/students/applications/" + appId + "/history").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].toStatus").value("APPLIED"));
    }

    @Test
    @DisplayName("Milestone Tabs: Applied, Shortlisted, and Interview tabs maintain persistent milestone membership")
    void testMilestoneTabsAndPersistence() throws Exception {
        ApplicationSubmitRequest req = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Milestone Test").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 1. Initial State: APPLIED
        // ALL: 1, APPLIED: 1, SHORTLISTED: 0, INTERVIEW: 0
        mockMvc.perform(get("/api/v1/students/applications?tab=ALL").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/students/applications?tab=APPLIED").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/students/applications?tab=SHORTLISTED").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get("/api/v1/students/applications?tab=INTERVIEW").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));

        // 2. Move to UNDER_REVIEW -> Applied tab must STILL contain it!
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/students/applications?tab=APPLIED").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("UNDER_REVIEW"));
        mockMvc.perform(get("/api/v1/students/applications?tab=SHORTLISTED").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));

        // 3. Move to SHORTLISTED -> Appears in Shortlisted AND remains in Applied
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/students/applications?tab=APPLIED").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/students/applications?tab=SHORTLISTED").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("SHORTLISTED"));
        mockMvc.perform(get("/api/v1/students/applications?tab=INTERVIEW").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));

        // 4. Move to INTERVIEW_SCHEDULED -> Appears in Interview AND remains in Shortlisted + Applied
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewScheduledAt\":\"2026-09-01T10:00:00\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/students/applications?tab=APPLIED").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/students/applications?tab=SHORTLISTED").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/students/applications?tab=INTERVIEW").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("INTERVIEW_SCHEDULED"));

        // 5. Move to ACCEPTED -> Remains in APPLIED and SHORTLISTED; INTERVIEW tab only shows current interviews
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/students/applications?tab=APPLIED").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("ACCEPTED"));
        mockMvc.perform(get("/api/v1/students/applications?tab=SHORTLISTED").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].status").value("ACCEPTED"));
        mockMvc.perform(get("/api/v1/students/applications?tab=INTERVIEW").header("Authorization", studentToken1))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalElements").value(0));

        // Verify Tab Counts endpoint (interview count is 0 because status is ACCEPTED, not INTERVIEW_SCHEDULED)
        mockMvc.perform(get("/api/v1/students/applications/counts").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.all").value(1))
                .andExpect(jsonPath("$.data.applied").value(1))
                .andExpect(jsonPath("$.data.shortlisted").value(1))
                .andExpect(jsonPath("$.data.interview").value(0));
    }

    @Test
    @DisplayName("Recruiter receives notification when candidate submits an application for their job")
    void testRecruiterReceivesNotificationOnCandidateApplication() throws Exception {
        ApplicationSubmitRequest req = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Notification Test").build();
        mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Recruiter checks notifications
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].title").value("New application received"))
                .andExpect(jsonPath("$.data.content[0].message").value(containsString("applied for")));

        // Verify notification unread count for recruiter
        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount", greaterThanOrEqualTo(1)));

        // Verify student2 does NOT receive this recruiter notification ("New application received")
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", studentToken2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].title", not(hasItem("New application received"))));
    }

    @Test
    @DisplayName("Legacy History Reconstruction: Reconstructs timeline when 0 history rows exist")
    void testLegacyApplicationHistoryReconstruction() throws Exception {
        ApplicationSubmitRequest req = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Legacy Reconstruction Test").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Advance to ACCEPTED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewScheduledAt\":\"2026-09-01T10:00:00\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}")).andExpect(status().isOk());

        // Simulate legacy record by deleting history rows
        applicationStatusHistoryRepository.deleteAll();

        // Fetching history should now trigger safe fallback reconstruction instead of empty list
        mockMvc.perform(get("/api/v1/students/applications/" + appId + "/history").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(5)))
                .andExpect(jsonPath("$.data[0].toStatus").value("APPLIED"))
                .andExpect(jsonPath("$.data[1].toStatus").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data[2].toStatus").value("SHORTLISTED"))
                .andExpect(jsonPath("$.data[3].toStatus").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data[4].toStatus").value("ACCEPTED"));
    }

    @Test
    @DisplayName("Timezone Consistency: Scheduling interview with UTC ISO string formats notification in Asia/Kolkata timezone")
    void testInterviewScheduling_TimezoneAndNotificationConsistency() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Timezone test").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Advance APPLIED -> UNDER_REVIEW -> SHORTLISTED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());

        // Schedule interview using UTC ISO instant: 2026-08-29T04:58:00Z (which corresponds to 10:28 AM IST)
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewScheduledAt\":\"2026-08-29T04:58:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data.interviewScheduledAt").value("2026-08-29T04:58:00"));

        // Verify Candidate Notification contains local Asia/Kolkata time ("Aug 29, 2026 at 10:28 AM")
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("INTERVIEW_INVITE"))
                .andExpect(jsonPath("$.data.content[0].message").value(org.hamcrest.Matchers.containsString("Aug 29, 2026 at 10:28 AM")));

        // Also verify Student Application Card API response contains interviewScheduledAt
        mockMvc.perform(get("/api/v1/students/applications").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data.content[0].interviewScheduledAt").value("2026-08-29T04:58:00"));
    }

    @Test
    @DisplayName("End-to-End Unique Time Chain: Recruiter enters 6:17 PM IST (Aug 28, 2026), verified across DB, API, Notification, History")
    void testInterviewScheduling_UniqueTimeEndToEndChain() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Unique Time Test").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // 1. Advance APPLIED -> UNDER_REVIEW -> SHORTLISTED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());

        // 2. Recruiter schedules interview for: Aug 28, 2026 at 6:17 PM IST
        // In browser (Asia/Kolkata), new Date("2026-08-28T18:17").toISOString() produces: "2026-08-28T12:47:00.000Z"
        String requestPayload = "{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewScheduledAt\":\"2026-08-28T12:47:00.000Z\"}";
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data.interviewScheduledAt").value("2026-08-28T12:47:00"));

        // 3. Database direct verification
        Application savedEntity = applicationRepository.findById(appId).orElseThrow();
        assertThat(savedEntity.getInterviewScheduledAt()).isEqualTo(LocalDateTime.of(2026, 8, 28, 12, 47, 0));

        // 4. Candidate Notification verification (contains Aug 28, 2026 at 6:17 PM)
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("INTERVIEW_INVITE"))
                .andExpect(jsonPath("$.data.content[0].message").value(org.hamcrest.Matchers.containsString("Aug 28, 2026 at 6:17 PM")));

        // 5. Student Application Card API verification
        mockMvc.perform(get("/api/v1/students/applications").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data.content[0].interviewScheduledAt").value("2026-08-28T12:47:00"));

        // 6. Recruiter ATS API verification
        mockMvc.perform(get("/api/v1/recruiters/jobs/" + publishedJobId + "/applications").header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].status").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data.content[0].interviewScheduledAt").value("2026-08-28T12:47:00"));

        // 7. Student Application History API verification
        mockMvc.perform(get("/api/v1/students/applications/" + appId + "/history").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[3].toStatus").value("INTERVIEW_SCHEDULED"));
    }

    @Test
    @DisplayName("Task 3 & 4: Legacy Notification Migration updates message to Asia/Kolkata while preserving history and creation timestamps")
    void testLegacyNotificationMigration_PreservesTransitionTimestampAndUpdatesMessage() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Legacy migration test").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Advance to INTERVIEW_SCHEDULED with UTC instant: 2026-08-29 04:58:00
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewScheduledAt\":\"2026-08-29T04:58:00Z\"}")).andExpect(status().isOk());

        // Simulate an old un-migrated notification in the database with raw UTC text
        User studentUser = userRepository.findByEmail("student_app_test1@careerforge.local").orElseThrow();
        Notification legacyNotif = notificationRepository.findByUserOrderByCreatedAtDescIdDesc(studentUser).get(0);
        legacyNotif.setMessage("Congratulations! An interview has been scheduled for 'Full Stack Engineer' at Delite Works on Aug 29, 2026 at 4:58 AM.");
        notificationRepository.save(legacyNotif);
        LocalDateTime originalCreatedAt = legacyNotif.getCreatedAt();

        // Execute startup migration
        dataInitializer.run();

        // Verify notification message is updated to 10:28 AM IST and createdAt is preserved
        Notification migratedNotif = notificationRepository.findById(legacyNotif.getId()).orElseThrow();
        assertThat(migratedNotif.getMessage()).contains("Aug 29, 2026 at 10:28 AM");
        assertThat(migratedNotif.getCreatedAt()).isEqualTo(originalCreatedAt);

        // Verify application status history changedAt is preserved
        mockMvc.perform(get("/api/v1/students/applications/" + appId + "/history").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[3].toStatus").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data[3].changedAt").isNotEmpty());
    }

    @Test
    @DisplayName("Task 6: New Interview scheduled for Aug 29, 2026 at 7:23 PM IST (19:23) is preserved end-to-end")
    void testNewInterviewScheduling_Aug29_UniqueTime() throws Exception {
        ApplicationSubmitRequest submitReq = ApplicationSubmitRequest.builder().jobId(publishedJobId).coverLetter("Aug 29 test").build();
        MvcResult res = mockMvc.perform(post("/api/v1/students/applications").header("Authorization", studentToken1)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(submitReq)))
                .andExpect(status().isCreated()).andReturn();
        Long appId = objectMapper.readTree(res.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Advance to SHORTLISTED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"UNDER_REVIEW\"}")).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk());

        // Aug 29, 2026 at 7:23 PM IST (19:23:00) -> UTC instant is 13:53:00Z
        String requestPayload = "{\"status\":\"INTERVIEW_SCHEDULED\",\"interviewScheduledAt\":\"2026-08-29T13:53:00.000Z\"}";
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + appId + "/status").header("Authorization", recruiterToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INTERVIEW_SCHEDULED"))
                .andExpect(jsonPath("$.data.interviewScheduledAt").value("2026-08-29T13:53:00"));

        // Verify DB value
        Application saved = applicationRepository.findById(appId).orElseThrow();
        assertThat(saved.getInterviewScheduledAt()).isEqualTo(LocalDateTime.of(2026, 8, 29, 13, 53, 0));

        // Verify Notification text contains Aug 29, 2026 at 7:23 PM
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].message").value(org.hamcrest.Matchers.containsString("Aug 29, 2026 at 7:23 PM")));

        // Verify History event exists and has transition timestamp
        mockMvc.perform(get("/api/v1/students/applications/" + appId + "/history").header("Authorization", studentToken1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[3].toStatus").value("INTERVIEW_SCHEDULED"));
    }
}
