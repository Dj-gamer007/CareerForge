package com.careerforge.controller;

import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.JobCreateRequest;
import com.careerforge.dto.request.JobSkillItemRequest;
import com.careerforge.dto.request.StudentProfileRequest;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.entity.Skill;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.ExperienceLevel;
import com.careerforge.entity.enums.JobType;
import com.careerforge.entity.enums.Role;
import com.careerforge.entity.enums.WorkMode;
import com.careerforge.repository.SkillRepository;
import com.careerforge.repository.UserRepository;
import com.careerforge.security.JwtTokenProvider;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.CompanyService;
import com.careerforge.service.JobService;
import com.careerforge.service.StudentProfileService;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudentSavedJobControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

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

    private String studentToken;
    private String recruiterToken;
    private Long publishedJobId;

    @BeforeEach
    void setUp() {
        User studentUser = userRepository.findByEmail("student_saved_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_saved_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        User recruiterUser = userRepository.findByEmail("recruiter_saved_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("recruiter_saved_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_RECRUITER)
                        .enabled(true)
                        .build()));

        studentToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser));
        recruiterToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterUser));

        // Create student profile
        studentProfileService.updateProfile(studentUser.getId(), StudentProfileRequest.builder()
                .firstName("Alice")
                .lastName("Student")
                .location("Bengaluru")
                .build());

        // Create company and job
        companyService.createCompany(recruiterUser.getId(), CompanyCreateRequest.builder()
                .name("AlphaTech Innovations")
                .industry("Software")
                .build());

        Skill javaSkill = skillRepository.findByNameIgnoreCase("Java")
                .orElseGet(() -> skillRepository.save(Skill.builder().name("Java").category("Backend").build()));

        JobCreateRequest jobReq = JobCreateRequest.builder()
                .title("Full Stack Engineer")
                .description("Build end to end scalable web applications.")
                .location("Bengaluru")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(BigDecimal.valueOf(1200000))
                .salaryMax(BigDecimal.valueOf(1800000))
                .deadline(LocalDateTime.now().plusDays(30))
                .skills(List.of(
                        JobSkillItemRequest.builder().skillId(javaSkill.getId()).isRequired(true).build()
                ))
                .build();

        JobDetailResponse draftJob = jobService.createJob(recruiterUser.getId(), jobReq);
        JobDetailResponse publishedJob = jobService.publishJob(recruiterUser.getId(), draftJob.getId());
        publishedJobId = publishedJob.getId();
    }

    @Test
    @DisplayName("Complete saved job flow: Save -> Check -> List -> Duplicate Reject -> Remove")
    void testSavedJobLifecycle() throws Exception {
        // 1. Check before saving -> false
        mockMvc.perform(get("/api/v1/students/saved-jobs/" + publishedJobId + "/check")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));

        // 2. Save job
        mockMvc.perform(post("/api/v1/students/saved-jobs/" + publishedJobId)
                        .header("Authorization", studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.jobId").value(publishedJobId))
                .andExpect(jsonPath("$.data.jobTitle").value("Full Stack Engineer"));

        // 3. Check after saving -> true
        mockMvc.perform(get("/api/v1/students/saved-jobs/" + publishedJobId + "/check")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // 4. List saved jobs
        mockMvc.perform(get("/api/v1/students/saved-jobs")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].jobId").value(publishedJobId));

        // 5. Duplicate save -> 400 Bad Request
        mockMvc.perform(post("/api/v1/students/saved-jobs/" + publishedJobId)
                        .header("Authorization", studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already saved")));

        // 6. Remove saved job
        mockMvc.perform(delete("/api/v1/students/saved-jobs/" + publishedJobId)
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());

        // 7. Check after removal -> false
        mockMvc.perform(get("/api/v1/students/saved-jobs/" + publishedJobId + "/check")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @DisplayName("Recruiter access to student saved jobs endpoint returns 403 Forbidden")
    void testRecruiterCannotAccessSavedJobs() throws Exception {
        mockMvc.perform(get("/api/v1/students/saved-jobs")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isForbidden());
    }
}
