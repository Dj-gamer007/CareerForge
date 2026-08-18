package com.careerforge.controller;

import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.JobCreateRequest;
import com.careerforge.dto.request.JobSkillItemRequest;
import com.careerforge.dto.request.JobUpdateRequest;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.entity.Skill;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.*;
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

        // Create company for first recruiter
        companyService.createCompany(recruiterUser.getId(), CompanyCreateRequest.builder()
                .name("TechCorp Solutions")
                .industry("Software")
                .build());

        // Create company for second recruiter
        companyService.createCompany(otherRecruiterUser.getId(), CompanyCreateRequest.builder()
                .name("OtherCorp Media")
                .industry("Media")
                .build());

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
}
