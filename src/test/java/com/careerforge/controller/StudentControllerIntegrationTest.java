package com.careerforge.controller;

import com.careerforge.dto.request.*;
import com.careerforge.dto.response.*;
import com.careerforge.entity.Skill;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.Role;
import com.careerforge.entity.enums.SkillProficiency;
import com.careerforge.repository.SkillRepository;
import com.careerforge.repository.UserRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudentControllerIntegrationTest {

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

    private String studentToken;
    private String recruiterToken;
    private User studentUser;
    private Skill testSkill;

    @BeforeEach
    void setUp() {
        // Create or load test student
        studentUser = userRepository.findByEmail("student_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        // Create or load test recruiter
        User recruiterUser = userRepository.findByEmail("recruiter_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("recruiter_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_RECRUITER)
                        .enabled(true)
                        .build()));

        // Ensure a skill exists
        testSkill = skillRepository.findByNameIgnoreCase("Java")
                .orElseGet(() -> skillRepository.save(Skill.builder()
                        .name("Java")
                        .category("Backend")
                        .build()));

        // Generate JWT tokens
        studentToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser));
        recruiterToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterUser));
    }

    // ==========================================
    // Security & Authorization Tests
    // ==========================================

    @Test
    @DisplayName("Unauthenticated request to Student API should return 401 Unauthorized")
    void testUnauthenticatedAccess_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/students/profile"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Recruiter access to Student API should return 403 Forbidden")
    void testRecruiterAccess_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/students/profile")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isForbidden());
    }

    // ==========================================
    // Profile Flow
    // ==========================================

    @Test
    @DisplayName("Student can create, retrieve, and update profile")
    void testStudentProfile_FullLifecycle() throws Exception {
        StudentProfileRequest createRequest = StudentProfileRequest.builder()
                .firstName("Alice")
                .lastName("Smith")
                .phone("+1234567890")
                .location("New York, NY")
                .bio("CS Graduate passionate about backend systems")
                .educationSummary("B.S. in Computer Science from NYU")
                .githubUrl("https://github.com/alicesmith")
                .linkedinUrl("https://linkedin.com/in/alicesmith")
                .build();

        // 1. Create Profile
        mockMvc.perform(post("/api/v1/students/profile")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Alice"))
                .andExpect(jsonPath("$.data.profileCompletionPercentage").value(greaterThan(0)));

        // 2. Retrieve Profile
        mockMvc.perform(get("/api/v1/students/profile")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lastName").value("Smith"))
                .andExpect(jsonPath("$.data.email").value("student_test@careerforge.local"));

        // 3. Update Profile
        createRequest.setBio("Updated bio: Senior CS student");
        mockMvc.perform(put("/api/v1/students/profile")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bio").value("Updated bio: Senior CS student"));
    }

    // ==========================================
    // Skills Flow
    // ==========================================

    @Test
    @DisplayName("Student can add skill, update proficiency, reject duplicate, and delete skill")
    void testStudentSkills_FullLifecycle() throws Exception {
        StudentSkillRequest skillRequest = StudentSkillRequest.builder()
                .skillId(testSkill.getId())
                .proficiency(SkillProficiency.INTERMEDIATE)
                .build();

        // 1. Add Skill
        mockMvc.perform(post("/api/v1/students/skills")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.skillName").value("Java"))
                .andExpect(jsonPath("$.data.proficiency").value("INTERMEDIATE"));

        // 2. Duplicate Skill Rejection
        mockMvc.perform(post("/api/v1/students/skills")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already added")));

        // 3. List Skills
        mockMvc.perform(get("/api/v1/students/skills")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 4. Update Proficiency
        skillRequest.setProficiency(SkillProficiency.EXPERT);
        mockMvc.perform(put("/api/v1/students/skills/" + testSkill.getId())
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(skillRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.proficiency").value("EXPERT"));

        // 5. Delete Skill
        mockMvc.perform(delete("/api/v1/students/skills/" + testSkill.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 6. Verify List is Empty
        mockMvc.perform(get("/api/v1/students/skills")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    // ==========================================
    // Education Flow
    // ==========================================

    @Test
    @DisplayName("Student can add, update, list, and delete education records")
    void testEducation_FullLifecycle() throws Exception {
        EducationRequest eduRequest = EducationRequest.builder()
                .institution("Stanford University")
                .degree("B.S.")
                .fieldOfStudy("Computer Science")
                .startDate(LocalDate.of(2021, 9, 1))
                .currentlyStudying(true)
                .gradeOrGpa("3.85")
                .build();

        // 1. Add Education
        MvcResult addResult = mockMvc.perform(post("/api/v1/students/education")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eduRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.institution").value("Stanford University"))
                .andReturn();

        EducationResponse addedEdu = objectMapper.readValue(
                objectMapper.readTree(addResult.getResponse().getContentAsString()).get("data").toString(),
                EducationResponse.class
        );

        // 2. List Education
        mockMvc.perform(get("/api/v1/students/education")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 3. Update Education
        eduRequest.setGradeOrGpa("3.95");
        mockMvc.perform(put("/api/v1/students/education/" + addedEdu.getId())
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(eduRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gradeOrGpa").value("3.95"));

        // 4. Delete Education
        mockMvc.perform(delete("/api/v1/students/education/" + addedEdu.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());
    }

    // ==========================================
    // Projects Flow
    // ==========================================

    @Test
    @DisplayName("Student can add, update, list, and delete project records")
    void testProjects_FullLifecycle() throws Exception {
        ProjectRequest projRequest = ProjectRequest.builder()
                .title("CareerForge Backend")
                .description("Intelligent recruitment platform backend")
                .technologies("Java, Spring Boot, MySQL, JWT")
                .githubUrl("https://github.com/example/careerforge")
                .startDate(LocalDate.of(2023, 1, 1))
                .build();

        // 1. Add Project
        MvcResult addResult = mockMvc.perform(post("/api/v1/students/projects")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("CareerForge Backend"))
                .andReturn();

        ProjectResponse addedProj = objectMapper.readValue(
                objectMapper.readTree(addResult.getResponse().getContentAsString()).get("data").toString(),
                ProjectResponse.class
        );

        // 2. List Projects
        mockMvc.perform(get("/api/v1/students/projects")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 3. Update Project
        projRequest.setTitle("CareerForge Fullstack");
        mockMvc.perform(put("/api/v1/students/projects/" + addedProj.getId())
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("CareerForge Fullstack"));

        // 4. Delete Project
        mockMvc.perform(delete("/api/v1/students/projects/" + addedProj.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());
    }

    // ==========================================
    // Certifications Flow
    // ==========================================

    @Test
    @DisplayName("Student can add, update, list, and delete certification records")
    void testCertifications_FullLifecycle() throws Exception {
        CertificationRequest certRequest = CertificationRequest.builder()
                .name("Oracle Certified Professional: Java SE 17 Developer")
                .issuingOrganization("Oracle")
                .issueDate(LocalDate.of(2023, 6, 1))
                .credentialId("OCP-JAVA-17")
                .build();

        // 1. Add Certification
        MvcResult addResult = mockMvc.perform(post("/api/v1/students/certifications")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(certRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Oracle Certified Professional: Java SE 17 Developer"))
                .andReturn();

        CertificationResponse addedCert = objectMapper.readValue(
                objectMapper.readTree(addResult.getResponse().getContentAsString()).get("data").toString(),
                CertificationResponse.class
        );

        // 2. List Certifications
        mockMvc.perform(get("/api/v1/students/certifications")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 3. Update Certification
        certRequest.setCredentialUrl("https://oracle.com/verify/OCP-JAVA-17");
        mockMvc.perform(put("/api/v1/students/certifications/" + addedCert.getId())
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(certRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.credentialUrl").value("https://oracle.com/verify/OCP-JAVA-17"));

        // 4. Delete Certification
        mockMvc.perform(delete("/api/v1/students/certifications/" + addedCert.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());
    }

    // ==========================================
    // Resume Management Flow
    // ==========================================

    @Test
    @DisplayName("Student can upload, retrieve, download, toggle active, and delete resume")
    void testResume_FullLifecycle() throws Exception {
        MockMultipartFile validPdf = new MockMultipartFile(
                "file",
                "my_resume.pdf",
                "application/pdf",
                "%PDF-1.4 sample resume content for testing".getBytes()
        );

        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "image.png",
                "image/png",
                "fake image content".getBytes()
        );

        // 1. Reject Non-PDF Upload
        mockMvc.perform(multipart("/api/v1/students/resumes")
                        .file(invalidFile)
                        .header("Authorization", studentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Only PDF files (.pdf) are supported")));

        // 2. Upload Valid PDF
        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/students/resumes")
                        .file(validPdf)
                        .header("Authorization", studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originalFileName").value("my_resume.pdf"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        ResumeResponse uploadedResume = objectMapper.readValue(
                objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("data").toString(),
                ResumeResponse.class
        );

        // 3. List Resumes
        mockMvc.perform(get("/api/v1/students/resumes")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 4. Download Resume
        mockMvc.perform(get("/api/v1/students/resumes/" + uploadedResume.getId() + "/download")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("my_resume.pdf")))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        // 5. Set Active
        mockMvc.perform(put("/api/v1/students/resumes/" + uploadedResume.getId() + "/active")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true));

        // 6. Delete Resume
        mockMvc.perform(delete("/api/v1/students/resumes/" + uploadedResume.getId())
                        .header("Authorization", studentToken))
                .andExpect(status().isOk());

        // 7. Verify Resume List Empty
        mockMvc.perform(get("/api/v1/students/resumes")
                        .header("Authorization", studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }
}
