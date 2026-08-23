package com.careerforge.controller;

import com.careerforge.dto.request.*;
import com.careerforge.dto.response.JobDetailResponse;
import com.careerforge.entity.Resume;
import com.careerforge.entity.Skill;
import com.careerforge.entity.StudentProfile;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.ApplicationStatus;
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
import com.careerforge.service.ApplicationService;
import com.careerforge.service.CompanyService;
import com.careerforge.service.JobService;
import com.careerforge.service.StorageService;
import com.careerforge.service.StudentProfileService;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecruiterApplicationIntegrationTest {

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

    @Autowired
    private ApplicationService applicationService;

    @MockBean
    private StorageService storageService;

    private String recruiterTokenA;
    private String recruiterTokenB;
    private String studentToken;
    private Long jobIdCompanyA;
    private Long applicationId;

    @BeforeEach
    void setUp() {
        // 1. Recruiter A (Company A)
        User recruiterA = userRepository.findByEmail("recruiter_ats_a@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("recruiter_ats_a@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_RECRUITER)
                        .enabled(true)
                        .build()));

        // 2. Recruiter B (Company B)
        User recruiterB = userRepository.findByEmail("recruiter_ats_b@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("recruiter_ats_b@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_RECRUITER)
                        .enabled(true)
                        .build()));

        // 3. Student
        User student = userRepository.findByEmail("student_ats@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_ats@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        recruiterTokenA = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterA));
        recruiterTokenB = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterB));
        studentToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(student));

        // Create companies
        companyService.createCompany(recruiterA.getId(), CompanyCreateRequest.builder().name("Company Alpha").industry("Tech").build());
        companyService.createCompany(recruiterB.getId(), CompanyCreateRequest.builder().name("Company Beta").industry("Finance").build());

        // Setup student profile + resume
        studentProfileService.updateProfile(student.getId(), StudentProfileRequest.builder()
                .firstName("Bob")
                .lastName("Coder")
                .location("Hyderabad")
                .bio("Full stack dev")
                .educationSummary("B.E. CSE")
                .build());

        StudentProfile sp = studentProfileRepository.findByUser_Id(student.getId()).orElseThrow();
        resumeRepository.save(Resume.builder()
                .studentProfile(sp)
                .originalFileName("bob_resume.pdf")
                .storedFileName("bob_uuid.pdf")
                .storagePath("/tmp/bob_uuid.pdf")
                .contentType("application/pdf")
                .fileSize(2048L)
                .isActive(true)
                .build());

        // Create published job for Company A
        Skill javaSkill = skillRepository.findByNameIgnoreCase("Java")
                .orElseGet(() -> skillRepository.save(Skill.builder().name("Java").category("Backend").build()));

        JobCreateRequest jobReq = JobCreateRequest.builder()
                .title("Senior Backend Developer")
                .description("Build microservices in Java.")
                .location("Hyderabad")
                .workMode(WorkMode.REMOTE)
                .jobType(JobType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR_LEVEL)
                .salaryMin(BigDecimal.valueOf(2500000))
                .salaryMax(BigDecimal.valueOf(3500000))
                .deadline(LocalDateTime.now().plusDays(45))
                .skills(List.of(JobSkillItemRequest.builder().skillId(javaSkill.getId()).isRequired(true).build()))
                .build();

        JobDetailResponse draftJob = jobService.createJob(recruiterA.getId(), jobReq);
        JobDetailResponse publishedJob = jobService.publishJob(recruiterA.getId(), draftJob.getId());
        jobIdCompanyA = publishedJob.getId();

        // Student applies to Company A job
        var appResp = applicationService.submitApplication(student.getId(), ApplicationSubmitRequest.builder()
                .jobId(jobIdCompanyA)
                .coverLetter("Very interested in this Senior Backend role.")
                .build());
        applicationId = appResp.getId();
    }

    @Test
    @DisplayName("Complete ATS pipeline: List -> Filter -> Detail -> Review -> Shortlist -> Schedule Interview -> Notes -> Accept -> Resume Download")
    void testRecruiterAtsPipeline() throws Exception {
        // 1. List applicants for job
        mockMvc.perform(get("/api/v1/recruiters/jobs/" + jobIdCompanyA + "/applications")
                        .header("Authorization", recruiterTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].candidateName").value("Bob Coder"))
                .andExpect(jsonPath("$.data.content[0].status").value("APPLIED"));

        // 2. Filter applicants by status
        mockMvc.perform(get("/api/v1/recruiters/jobs/" + jobIdCompanyA + "/applications?status=APPLIED")
                        .header("Authorization", recruiterTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)));

        mockMvc.perform(get("/api/v1/recruiters/jobs/" + jobIdCompanyA + "/applications?status=ACCEPTED")
                        .header("Authorization", recruiterTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // 3. View applicant dossier
        mockMvc.perform(get("/api/v1/recruiters/applications/" + applicationId)
                        .header("Authorization", recruiterTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidateName").value("Bob Coder"))
                .andExpect(jsonPath("$.data.coverLetter").value("Very interested in this Senior Backend role."))
                .andExpect(jsonPath("$.data.skillBreakdown").exists());

        // 4. Transition APPLIED -> UNDER_REVIEW
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.UNDER_REVIEW)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"))
                .andExpect(jsonPath("$.data.reviewedAt").exists());

        // 5. Transition UNDER_REVIEW -> SHORTLISTED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.SHORTLISTED)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHORTLISTED"));

        // 6. Transition SHORTLISTED -> INTERVIEW_SCHEDULED (future timestamp)
        LocalDateTime futureInterview = LocalDateTime.of(2026, 8, 25, 14, 30);
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.INTERVIEW_SCHEDULED)
                                .interviewScheduledAt(futureInterview)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INTERVIEW_SCHEDULED"));

        // Verify candidate received interview invitation notification with human-readable time format
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title", containsString("Interview Invitation")))
                .andExpect(jsonPath("$.data.content[0].message", containsString("Aug 25, 2026 at 2:30 PM")));

        // 7. Update recruiter notes
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/notes")
                        .header("Authorization", recruiterTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationNotesRequest.builder()
                                .recruiterNotes("Candidate demonstrated strong problem solving skills.")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recruiterNotes").value("Candidate demonstrated strong problem solving skills."));

        // 8. Transition INTERVIEW_SCHEDULED -> ACCEPTED
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.ACCEPTED)
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        // 9. Download applicant resume
        when(storageService.loadAsResource(anyString())).thenReturn(new ByteArrayResource("PDF-DATA".getBytes()));

        mockMvc.perform(get("/api/v1/recruiters/applications/" + applicationId + "/resume/download")
                        .header("Authorization", recruiterTokenA))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("bob_resume.pdf")));
    }

    @Test
    @DisplayName("Cross-company recruiter cannot access or mutate applicant -> 404 Not Found")
    void testCrossCompanyRecruiterAccess_Returns404() throws Exception {
        // Recruiter B attempts to view Recruiter A's job application
        mockMvc.perform(get("/api/v1/recruiters/applications/" + applicationId)
                        .header("Authorization", recruiterTokenB))
                .andExpect(status().isNotFound());

        // Recruiter B attempts to update status
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/status")
                        .header("Authorization", recruiterTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ApplicationStatusUpdateRequest.builder()
                                .status(ApplicationStatus.REJECTED)
                                .build())))
                .andExpect(status().isNotFound());

        // Recruiter B attempts to download resume
        mockMvc.perform(get("/api/v1/recruiters/applications/" + applicationId + "/resume/download")
                        .header("Authorization", recruiterTokenB))
                .andExpect(status().isNotFound());

        // Recruiter B attempts to update notes -> 404
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/notes")
                        .header("Authorization", recruiterTokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Unauthorized private notes\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Recruiter can save internal notes using 'notes' payload alias and notes persist")
    void testSaveInternalNotes_WithNotesAlias_Success() throws Exception {
        mockMvc.perform(patch("/api/v1/recruiters/applications/" + applicationId + "/notes")
                        .header("Authorization", recruiterTokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Not capable\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recruiterNotes").value("Not capable"));

        // Fetch application detail again and verify persistence
        mockMvc.perform(get("/api/v1/recruiters/applications/" + applicationId)
                        .header("Authorization", recruiterTokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recruiterNotes").value("Not capable"));
    }

    @Test
    @DisplayName("Student access to recruiter application endpoint returns 403 Forbidden")
    void testStudentCannotAccessRecruiterEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/recruiters/applications/" + applicationId)
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }
}
