package com.careerforge.controller;

import com.careerforge.dto.request.ApplicationSubmitRequest;
import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.JobCreateRequest;
import com.careerforge.dto.request.JobSkillItemRequest;
import com.careerforge.dto.request.StudentProfileRequest;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.entity.Resume;
import com.careerforge.entity.Skill;
import com.careerforge.entity.StudentProfile;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.ExperienceLevel;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.Role;
import com.careerforge.entity.enums.WorkMode;
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
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private ResumeRepository resumeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private StudentProfileService studentProfileService;

    @Autowired
    private CompanyService companyService;

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
        companyService.createCompany(recruiterUser.getId(), CompanyCreateRequest.builder()
                .name("NextGen Software Corp")
                .industry("Cloud Computing")
                .build());

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
}
