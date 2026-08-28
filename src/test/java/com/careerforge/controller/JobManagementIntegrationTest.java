package com.careerforge.controller;

import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.JobCreateRequest;
import com.careerforge.dto.request.JobSkillItemRequest;
import com.careerforge.dto.request.JobUpdateRequest;
import com.careerforge.dto.response.CompanyResponse;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.entity.Skill;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.*;
import com.careerforge.repository.CompanyRepository;
import com.careerforge.repository.NotificationRepository;
import com.careerforge.repository.SkillRepository;
import com.careerforge.repository.UserRepository;
import com.careerforge.security.JwtTokenProvider;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.CompanyService;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JobManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private String recruiterToken;
    private String otherRecruiterToken;
    private User recruiterUser;
    private User otherRecruiterUser;
    private Skill javaSkill;

    @BeforeEach
    void setUp() {
        recruiterUser = userRepository.findByEmail("recruiter_job_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("recruiter_job_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_RECRUITER)
                        .enabled(true)
                        .build()));

        otherRecruiterUser = userRepository.findByEmail("other_recruiter_job_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("other_recruiter_job_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_RECRUITER)
                        .enabled(true)
                        .build()));

        javaSkill = skillRepository.findByNameIgnoreCase("Java")
                .orElseGet(() -> skillRepository.save(Skill.builder().name("Java").category("Backend").build()));

        // Create company for first recruiter and verify for job publishing test
        CompanyResponse comp1 = companyService.createCompany(recruiterUser.getId(), CompanyCreateRequest.builder()
                .name("TechCorp Solutions")
                .industry("Software")
                .build());
        companyRepository.findById(comp1.getId()).ifPresent(c -> {
            c.setVerificationStatus(com.careerforge.entity.enums.CompanyVerificationStatus.VERIFIED);
            companyRepository.save(c);
        });

        // Create company for second recruiter and verify for job publishing test
        CompanyResponse comp2 = companyService.createCompany(otherRecruiterUser.getId(), CompanyCreateRequest.builder()
                .name("OtherCorp Media")
                .industry("Media")
                .build());
        companyRepository.findById(comp2.getId()).ifPresent(c -> {
            c.setVerificationStatus(com.careerforge.entity.enums.CompanyVerificationStatus.VERIFIED);
            companyRepository.save(c);
        });

        recruiterToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterUser));
        otherRecruiterToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(otherRecruiterUser));
    }

    @Test
    @DisplayName("Complete job lifecycle: Draft -> Publish -> Unpublish -> Publish -> Close -> Reopen -> Archive -> Delete")
    void testCompleteJobLifecycle() throws Exception {
        JobCreateRequest createReq = JobCreateRequest.builder()
                .title("Backend Java Engineer")
                .description("Build core distributed systems with Spring Boot.")
                .location("Bengaluru, India")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(new BigDecimal("1500000"))
                .salaryMax(new BigDecimal("2200000"))
                .currency("INR")
                .deadline(LocalDateTime.now().plusDays(30))
                .skills(List.of(
                        JobSkillItemRequest.builder()
                                .skillId(javaSkill.getId())
                                .isRequired(true)
                                .minimumProficiency(SkillProficiency.ADVANCED)
                                .build()
                ))
                .build();

        // 1. Create Job (DRAFT)
        MvcResult createResult = mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.currency").value("INR"))
                .andExpect(jsonPath("$.data.skills", hasSize(1)))
                .andReturn();

        JobDetailResponse createdJob = objectMapper.readValue(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").toString(),
                JobDetailResponse.class
        );
        Long jobId = createdJob.getId();

        // 2. Publish Job
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/publish")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 3. Unpublish Job back to DRAFT
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/unpublish")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // 4. Publish again
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/publish")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 5. Close Job
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/close")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // 6. Reopen Job
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/reopen")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 7. Close and Archive Job
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/close")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/archive")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        // 8. Delete Archived Job
        mockMvc.perform(delete("/api/v1/recruiters/jobs/" + jobId)
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Recruiter cannot modify another company's job (404 / Cross-company isolation)")
    void testCrossCompanyJobIsolation() throws Exception {
        // Create job by Recruiter 1
        JobCreateRequest createReq = JobCreateRequest.builder()
                .title("TechCorp Exclusive Job")
                .description("Private description for TechCorp.")
                .location("Hyderabad")
                .workMode(WorkMode.ONSITE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JobDetailResponse createdJob = objectMapper.readValue(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").toString(),
                JobDetailResponse.class
        );

        // Recruiter 2 tries to update Recruiter 1's job -> 404
        JobUpdateRequest updateReq = JobUpdateRequest.builder()
                .title("Hacked Title")
                .description("Hacked description")
                .location("Hyderabad")
                .workMode(WorkMode.ONSITE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .build();

        mockMvc.perform(put("/api/v1/recruiters/jobs/" + createdJob.getId())
                        .header("Authorization", otherRecruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Recruiter cannot publish another company's job (404)")
    void testPublishUnauthorizedJob_ReturnsNotFound() throws Exception {
        JobCreateRequest createReq = JobCreateRequest.builder()
                .title("TechCorp Private Job")
                .description("Private description for TechCorp.")
                .location("Hyderabad")
                .workMode(WorkMode.ONSITE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JobDetailResponse createdJob = objectMapper.readValue(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").toString(),
                JobDetailResponse.class
        );

        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + createdJob.getId() + "/publish")
                        .header("Authorization", otherRecruiterToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Publishing non-existent job returns 404")
    void testPublishNonExistentJob_ReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/v1/recruiters/jobs/999999/publish")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Creating job with 1 skill and publishing on first attempt succeeds")
    void testCreateJobWithSkill_PublishFirstTime_Succeeds() throws Exception {
        String jobJson = "{" +
                "\"title\":\"Senior React Developer\"," +
                "\"description\":\"Expert React Engineer with TypeScript and state management.\"," +
                "\"location\":\"Bengaluru, India\"," +
                "\"workMode\":\"REMOTE\"," +
                "\"jobType\":\"FULL_TIME\"," +
                "\"experienceLevel\":\"SENIOR_LEVEL\"," +
                "\"skills\":[{\"skillId\":" + javaSkill.getId() + ",\"required\":true,\"minimumProficiency\":\"INTERMEDIATE\"}]" +
                "}";

        MvcResult createResult = mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jobJson))
                .andExpect(status().isCreated())
                .andReturn();

        JobDetailResponse createdJob = objectMapper.readValue(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").toString(),
                JobDetailResponse.class
        );

        // Publish for the first time
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + createdJob.getId() + "/publish")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("Publishing job with zero skills fails validation (400 Bad Request)")
    void testPublishJobWithZeroSkills_FailsValidation() throws Exception {
        JobCreateRequest createReq = JobCreateRequest.builder()
                .title("No Skills Job")
                .description("Job without any skills attached.")
                .location("Bengaluru, India")
                .workMode(WorkMode.ONSITE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.ENTRY_LEVEL)
                .skills(List.of())
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JobDetailResponse createdJob = objectMapper.readValue(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").toString(),
                JobDetailResponse.class
        );

        // Attempting to publish must fail with 400
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + createdJob.getId() + "/publish")
                .header("Authorization", recruiterToken))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("at least one required skill")));
    }

    @Test
    @DisplayName("Job publication notifies students only, persists in db, and avoids duplicate notifications")
    void testJobPublicationNotifications_StudentOnly_Persisted_NoDuplicates() throws Exception {
        // Setup 2 students and 1 admin
        User student1 = userRepository.findByEmail("student_notif_1@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_notif_1@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        User student2 = userRepository.findByEmail("student_notif_2@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_notif_2@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        User adminUser = userRepository.findByEmail("admin_notif@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("admin_notif@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_ADMIN)
                        .enabled(true)
                        .build()));

        long student1InitialNotifs = notificationRepository.countByUser_IdAndIsReadFalse(student1.getId());
        long student2InitialNotifs = notificationRepository.countByUser_IdAndIsReadFalse(student2.getId());
        long recruiterInitialNotifs = notificationRepository.countByUser_IdAndIsReadFalse(recruiterUser.getId());
        long adminInitialNotifs = notificationRepository.countByUser_IdAndIsReadFalse(adminUser.getId());

        // 1. Create a draft job -> No notification should be created
        JobCreateRequest jobReq = JobCreateRequest.builder()
                .title("Full Stack Cloud Architect")
                .description("Build enterprise cloud applications.")
                .location("Bengaluru, India")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .skills(List.of(JobSkillItemRequest.builder().skillId(javaSkill.getId()).isRequired(true).build()))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andReturn();

        Long jobId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("data").get("id").asLong();

        // Check that draft creation didn't create student notifications, but notified admin
        org.junit.jupiter.api.Assertions.assertEquals(student1InitialNotifs, notificationRepository.countByUser_IdAndIsReadFalse(student1.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(student2InitialNotifs, notificationRepository.countByUser_IdAndIsReadFalse(student2.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(adminInitialNotifs + 1, notificationRepository.countByUser_IdAndIsReadFalse(adminUser.getId()));

        // 2. Publish Job -> Notification should be sent to students only
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/publish")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // Verify students received notification
        org.junit.jupiter.api.Assertions.assertEquals(student1InitialNotifs + 1, notificationRepository.countByUser_IdAndIsReadFalse(student1.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(student2InitialNotifs + 1, notificationRepository.countByUser_IdAndIsReadFalse(student2.getId()));

        // Verify recruiter did NOT receive notification, and admin did NOT receive student job notification
        org.junit.jupiter.api.Assertions.assertEquals(recruiterInitialNotifs, notificationRepository.countByUser_IdAndIsReadFalse(recruiterUser.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(adminInitialNotifs + 1, notificationRepository.countByUser_IdAndIsReadFalse(adminUser.getId()));

        // Check notification content in Student 1's notifications
        var student1Notifs = notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(student1.getId(), org.springframework.data.domain.Pageable.unpaged());
        var latestNotif = student1Notifs.getContent().get(0);
        org.junit.jupiter.api.Assertions.assertEquals("New Job Posted", latestNotif.getTitle());
        org.junit.jupiter.api.Assertions.assertTrue(latestNotif.getMessage().contains("Full Stack Cloud Architect"));
        org.junit.jupiter.api.Assertions.assertTrue(latestNotif.getMessage().contains("TechCorp Solutions"));

        // 3. Attempting duplicate publication should fail and NOT create duplicate notifications
        mockMvc.perform(patch("/api/v1/recruiters/jobs/" + jobId + "/publish")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(student1InitialNotifs + 1, notificationRepository.countByUser_IdAndIsReadFalse(student1.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(student2InitialNotifs + 1, notificationRepository.countByUser_IdAndIsReadFalse(student2.getId()));
    }

    @Test
    @DisplayName("Job creation notifies only active Admins of job pending moderation, recipient isolation verified")
    void testJobCreation_NotifiesOnlyActiveAdmins() throws Exception {
        // Setup 2 active Admins, 1 disabled Admin, 1 Student
        User activeAdmin1 = userRepository.findByEmail("admin1_job_mod_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("admin1_job_mod_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_ADMIN)
                        .enabled(true)
                        .build()));

        User activeAdmin2 = userRepository.findByEmail("admin2_job_mod_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("admin2_job_mod_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_ADMIN)
                        .enabled(true)
                        .build()));

        User disabledAdmin = userRepository.findByEmail("admin_disabled_job_mod_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("admin_disabled_job_mod_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_ADMIN)
                        .enabled(false)
                        .build()));

        User student = userRepository.findByEmail("student_job_mod_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_job_mod_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        long admin1InitialCount = notificationRepository.countByUser_IdAndIsReadFalse(activeAdmin1.getId());
        long admin2InitialCount = notificationRepository.countByUser_IdAndIsReadFalse(activeAdmin2.getId());
        long disabledAdminInitialCount = notificationRepository.countByUser_IdAndIsReadFalse(disabledAdmin.getId());
        long recruiterInitialCount = notificationRepository.countByUser_IdAndIsReadFalse(recruiterUser.getId());
        long studentInitialCount = notificationRepository.countByUser_IdAndIsReadFalse(student.getId());

        JobCreateRequest jobReq = JobCreateRequest.builder()
                .title("Staff Distributed Systems Engineer")
                .description("Build globally distributed database storage engine.")
                .location("Bengaluru, India")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .skills(List.of(JobSkillItemRequest.builder().skillId(javaSkill.getId()).isRequired(true).build()))
                .build();

        // 1. Recruiter creates job
        mockMvc.perform(post("/api/v1/recruiters/jobs")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jobReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        // 2. Active admins received moderation notification
        org.junit.jupiter.api.Assertions.assertEquals(admin1InitialCount + 1, notificationRepository.countByUser_IdAndIsReadFalse(activeAdmin1.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(admin2InitialCount + 1, notificationRepository.countByUser_IdAndIsReadFalse(activeAdmin2.getId()));

        // 3. Disabled admin, recruiter, and student received NO notifications on job creation
        org.junit.jupiter.api.Assertions.assertEquals(disabledAdminInitialCount, notificationRepository.countByUser_IdAndIsReadFalse(disabledAdmin.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(recruiterInitialCount, notificationRepository.countByUser_IdAndIsReadFalse(recruiterUser.getId()));
        org.junit.jupiter.api.Assertions.assertEquals(studentInitialCount, notificationRepository.countByUser_IdAndIsReadFalse(student.getId()));

        // 4. Verify notification content
        var notifs = notificationRepository.findAllByUser_IdOrderByCreatedAtDesc(activeAdmin1.getId(), org.springframework.data.domain.Pageable.unpaged());
        var latest = notifs.getContent().get(0);
        org.junit.jupiter.api.Assertions.assertEquals("New Job Pending Moderation", latest.getTitle());
        org.junit.jupiter.api.Assertions.assertTrue(latest.getMessage().contains("Staff Distributed Systems Engineer"));
        org.junit.jupiter.api.Assertions.assertTrue(latest.getMessage().contains("TechCorp Solutions"));
        org.junit.jupiter.api.Assertions.assertTrue(latest.getMessage().contains("requires review"));
    }
}
