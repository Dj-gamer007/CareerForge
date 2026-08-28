package com.careerforge.controller;

import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.JobCreateRequest;
import com.careerforge.dto.request.JobSkillItemRequest;
import com.careerforge.dto.response.CompanyResponse;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.entity.Skill;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.*;
import com.careerforge.repository.CompanyRepository;
import com.careerforge.repository.SkillRepository;
import com.careerforge.repository.UserRepository;
import com.careerforge.security.JwtTokenProvider;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.CompanyService;
import com.careerforge.service.JobService;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JobDiscoveryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private JobService jobService;

    private Skill javaSkill;
    private Skill reactSkill;
    private Long publishedJobId;
    private String publishedJobSlug;
    private User recruiter;

    @BeforeEach
    void setUp() {
        recruiter = userRepository.findByEmail("recruiter_discovery@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("recruiter_discovery@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_RECRUITER)
                        .enabled(true)
                        .build()));

        javaSkill = skillRepository.findByNameIgnoreCase("Java")
                .orElseGet(() -> skillRepository.save(Skill.builder().name("Java").category("Backend").build()));

        reactSkill = skillRepository.findByNameIgnoreCase("React")
                .orElseGet(() -> skillRepository.save(Skill.builder().name("React").category("Frontend").build()));

        // Create company and verify for public discovery tests
        CompanyResponse comp = companyService.createCompany(recruiter.getId(), CompanyCreateRequest.builder()
                .name("Global Systems Inc")
                .industry("Enterprise Software")
                .location("Bengaluru, India")
                .build());
        companyRepository.findById(comp.getId()).ifPresent(c -> {
            c.setVerificationStatus(com.careerforge.entity.enums.CompanyVerificationStatus.VERIFIED);
            companyRepository.save(c);
        });

        // Create and publish a job
        JobCreateRequest jobReq1 = JobCreateRequest.builder()
                .title("Senior Java Architect")
                .description("Lead architecture and development of scalable microservices in Java.")
                .location("Bengaluru, India")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .salaryMin(new BigDecimal("2500000"))
                .salaryMax(new BigDecimal("3500000"))
                .currency("INR")
                .deadline(LocalDateTime.now().plusDays(45))
                .skills(List.of(
                        JobSkillItemRequest.builder().skillId(javaSkill.getId()).isRequired(true).minimumProficiency(SkillProficiency.EXPERT).build()
                ))
                .build();

        JobDetailResponse draftJob = jobService.createJob(recruiter.getId(), jobReq1);
        JobDetailResponse publishedJob = jobService.publishJob(recruiter.getId(), draftJob.getId());
        publishedJobId = publishedJob.getId();
        publishedJobSlug = publishedJob.getSlug();

        // Create a DRAFT job (should not show up in public search)
        JobCreateRequest jobReq2 = JobCreateRequest.builder()
                .title("Unpublished Frontend Draft")
                .description("React frontend role still in draft mode.")
                .location("Remote")
                .workMode(WorkMode.REMOTE)
                .jobType(JobType.CONTRACT)
                .experienceLevel(ExperienceLevel.ENTRY_LEVEL)
                .skills(List.of(
                        JobSkillItemRequest.builder().skillId(reactSkill.getId()).isRequired(true).minimumProficiency(SkillProficiency.BEGINNER).build()
                ))
                .build();
        jobService.createJob(recruiter.getId(), jobReq2);
    }

    @Test
    @DisplayName("Public user can search and filter published jobs without authentication")
    void testPublicJobSearch() throws Exception {
        // 1. Search all (only 1 published job exists)
        mockMvc.perform(get("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Senior Java Architect"))
                .andExpect(jsonPath("$.data.content[0].currency").value("INR"));

        // 2. Keyword match
        mockMvc.perform(get("/api/v1/jobs?keyword=Architect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        // 3. Keyword mismatch
        mockMvc.perform(get("/api/v1/jobs?keyword=NonExistentJobTitleXYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // 4. WorkMode filter
        mockMvc.perform(get("/api/v1/jobs?workModes=HYBRID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        mockMvc.perform(get("/api/v1/jobs?workModes=ONSITE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // 5. Skill filter
        mockMvc.perform(get("/api/v1/jobs?skillIds=" + javaSkill.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        mockMvc.perform(get("/api/v1/jobs?skillIds=" + reactSkill.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));
    }

    @Test
    @DisplayName("Public user can view published job details by ID and by Slug")
    void testPublicJobDetail() throws Exception {
        // By ID
        mockMvc.perform(get("/api/v1/jobs/" + publishedJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Senior Java Architect"))
                .andExpect(jsonPath("$.data.companyName").value("Global Systems Inc"))
                .andExpect(jsonPath("$.data.skills", hasSize(1)));

        // By Slug
        mockMvc.perform(get("/api/v1/jobs/slug/" + publishedJobSlug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(publishedJobId));
    }

    @Test
    @DisplayName("General search matches required AND optional skills case-insensitively without duplicate results")
    void testGeneralSearch_MatchesRequiredAndOptionalSkillsCaseInsensitively() throws Exception {
        User recruiter = userRepository.findByEmail("recruiter_discovery@careerforge.local").orElseThrow();

        Skill pythonSkill = skillRepository.findByNameIgnoreCase("Python")
                .orElseGet(() -> skillRepository.save(Skill.builder().name("Python").category("Backend").build()));
        Skill mysqlSkill = skillRepository.findByNameIgnoreCase("MySQL")
                .orElseGet(() -> skillRepository.save(Skill.builder().name("MySQL").category("Database").build()));
        Skill dockerSkill = skillRepository.findByNameIgnoreCase("Docker")
                .orElseGet(() -> skillRepository.save(Skill.builder().name("Docker").category("DevOps").build()));
        Skill springBootSkill = skillRepository.findByNameIgnoreCase("Spring Boot")
                .orElseGet(() -> skillRepository.save(Skill.builder().name("Spring Boot").category("Backend").build()));

        // Create a multi-skill job where description and title DO NOT mention MySQL, Docker, or Spring Boot
        JobCreateRequest multiSkillReq = JobCreateRequest.builder()
                .title("Cloud Systems Specialist")
                .description("Build high-performance distributed backend services.")
                .location("Hyderabad, India")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.MID_LEVEL)
                .salaryMin(new BigDecimal("1800000"))
                .salaryMax(new BigDecimal("2400000"))
                .currency("INR")
                .deadline(LocalDateTime.now().plusDays(30))
                .skills(List.of(
                        // Required skills: Java, Python
                        JobSkillItemRequest.builder().skillId(javaSkill.getId()).isRequired(true).minimumProficiency(SkillProficiency.ADVANCED).build(),
                        JobSkillItemRequest.builder().skillId(pythonSkill.getId()).isRequired(true).minimumProficiency(SkillProficiency.INTERMEDIATE).build(),
                        // Optional skills: MySQL, Docker, Spring Boot
                        JobSkillItemRequest.builder().skillId(mysqlSkill.getId()).isRequired(false).minimumProficiency(SkillProficiency.INTERMEDIATE).build(),
                        JobSkillItemRequest.builder().skillId(dockerSkill.getId()).isRequired(false).minimumProficiency(SkillProficiency.INTERMEDIATE).build(),
                        JobSkillItemRequest.builder().skillId(springBootSkill.getId()).isRequired(false).minimumProficiency(SkillProficiency.ADVANCED).build()
                ))
                .build();

        JobDetailResponse draft = jobService.createJob(recruiter.getId(), multiSkillReq);
        jobService.publishJob(recruiter.getId(), draft.getId());

        // A. Required Skill: Java
        mockMvc.perform(get("/api/v1/jobs?keyword=Java"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2))); // 2 jobs match Java (both Senior Java Architect and Cloud Systems Specialist)

        // B. Required Skill: Python
        mockMvc.perform(get("/api/v1/jobs?keyword=Python"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));

        // C. Optional Skill: MySQL (with various casing)
        mockMvc.perform(get("/api/v1/jobs?keyword=MySQL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));

        mockMvc.perform(get("/api/v1/jobs?keyword=mysql"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));

        mockMvc.perform(get("/api/v1/jobs?keyword=MYSQL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));

        // D. Optional Skill: Docker
        mockMvc.perform(get("/api/v1/jobs?keyword=Docker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));

        mockMvc.perform(get("/api/v1/jobs?keyword=docker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));

        // E. Optional Skill: Spring Boot
        mockMvc.perform(get("/api/v1/jobs?keyword=Spring Boot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));

        mockMvc.perform(get("/api/v1/jobs?keyword=spring boot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));

        mockMvc.perform(get("/api/v1/jobs?keyword=SPRING BOOT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));

        // F. Non-existent skill / keyword
        mockMvc.perform(get("/api/v1/jobs?keyword=KubernetesXYZNotFound"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // G. Dedicated skill filter still works for optional skills
        mockMvc.perform(get("/api/v1/jobs?skillIds=" + mysqlSkill.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));

        mockMvc.perform(get("/api/v1/jobs?skillIds=" + dockerSkill.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Cloud Systems Specialist"));
    }

    @Test
    @DisplayName("Should serialize required and optional skills with accurate boolean flags and minimum proficiencies in /api/v1/jobs/{slug}")
    void testJobDetail_MixedSkillsRequiredOptionalFlagsPreserved() throws Exception {
        Skill java = skillRepository.findByNameIgnoreCase("Java").orElseGet(() -> skillRepository.save(Skill.builder().name("Java").category("Backend").build()));
        Skill python = skillRepository.findByNameIgnoreCase("Python").orElseGet(() -> skillRepository.save(Skill.builder().name("Python").category("Backend").build()));
        Skill mysql = skillRepository.findByNameIgnoreCase("MySQL").orElseGet(() -> skillRepository.save(Skill.builder().name("MySQL").category("Database").build()));
        Skill docker = skillRepository.findByNameIgnoreCase("Docker").orElseGet(() -> skillRepository.save(Skill.builder().name("Docker").category("DevOps").build()));
        Skill springBoot = skillRepository.findByNameIgnoreCase("Spring Boot").orElseGet(() -> skillRepository.save(Skill.builder().name("Spring Boot").category("Backend").build()));

        JobCreateRequest mixedJobReq = JobCreateRequest.builder()
                .title("Staff Platform Engineer")
                .description("Build core platforms.")
                .location("Hyderabad, India")
                .workMode(WorkMode.HYBRID)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .currency("INR")
                .salaryMin(new BigDecimal("2500000"))
                .salaryMax(new BigDecimal("3500000"))
                .deadline(LocalDateTime.now().plusDays(30))
                .skills(List.of(
                        JobSkillItemRequest.builder().skillId(java.getId()).isRequired(true).minimumProficiency(SkillProficiency.INTERMEDIATE).build(),
                        JobSkillItemRequest.builder().skillId(python.getId()).isRequired(true).minimumProficiency(SkillProficiency.INTERMEDIATE).build(),
                        JobSkillItemRequest.builder().skillId(mysql.getId()).isRequired(false).minimumProficiency(SkillProficiency.INTERMEDIATE).build(),
                        JobSkillItemRequest.builder().skillId(docker.getId()).isRequired(false).minimumProficiency(SkillProficiency.BEGINNER).build(),
                        JobSkillItemRequest.builder().skillId(springBoot.getId()).isRequired(true).minimumProficiency(SkillProficiency.ADVANCED).build()
                ))
                .build();

        JobDetailResponse created = jobService.createJob(recruiter.getId(), mixedJobReq);
        jobService.publishJob(recruiter.getId(), created.getId());

        mockMvc.perform(get("/api/v1/jobs/slug/" + created.getSlug()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skills", hasSize(5)))
                // Java: REQUIRED, INTERMEDIATE
                .andExpect(jsonPath("$.data.skills[0].skillName").value("Java"))
                .andExpect(jsonPath("$.data.skills[0].required").value(true))
                .andExpect(jsonPath("$.data.skills[0].minimumProficiency").value("INTERMEDIATE"))
                // Python: REQUIRED, INTERMEDIATE
                .andExpect(jsonPath("$.data.skills[1].skillName").value("Python"))
                .andExpect(jsonPath("$.data.skills[1].required").value(true))
                .andExpect(jsonPath("$.data.skills[1].minimumProficiency").value("INTERMEDIATE"))
                // MySQL: OPTIONAL, INTERMEDIATE
                .andExpect(jsonPath("$.data.skills[2].skillName").value("MySQL"))
                .andExpect(jsonPath("$.data.skills[2].required").value(false))
                .andExpect(jsonPath("$.data.skills[2].minimumProficiency").value("INTERMEDIATE"))
                // Docker: OPTIONAL, BEGINNER
                .andExpect(jsonPath("$.data.skills[3].skillName").value("Docker"))
                .andExpect(jsonPath("$.data.skills[3].required").value(false))
                .andExpect(jsonPath("$.data.skills[3].minimumProficiency").value("BEGINNER"))
                // Spring Boot: REQUIRED, ADVANCED
                .andExpect(jsonPath("$.data.skills[4].skillName").value("Spring Boot"))
                .andExpect(jsonPath("$.data.skills[4].required").value(true))
                .andExpect(jsonPath("$.data.skills[4].minimumProficiency").value("ADVANCED"));
    }
}
