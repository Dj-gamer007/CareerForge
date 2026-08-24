package com.careerforge.controller;

import com.careerforge.dto.request.CompanyCreateRequest;
import com.careerforge.dto.request.CompanyUpdateRequest;
import com.careerforge.entity.Company;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.Role;
import com.careerforge.repository.CompanyRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanyControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String recruiterToken;
    private String studentToken;
    private User recruiterUser;
    private User studentUser;

    @BeforeEach
    void setUp() {
        recruiterUser = userRepository.findByEmail("recruiter_comp_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("recruiter_comp_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_RECRUITER)
                        .enabled(true)
                        .build()));

        studentUser = userRepository.findByEmail("student_comp_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_comp_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        recruiterToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterUser));
        studentToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser));
    }

    @Test
    @DisplayName("Recruiter can create company, fetch own company, and update it")
    void testCompanyLifecycle_Success() throws Exception {
        CompanyCreateRequest createReq = CompanyCreateRequest.builder()
                .name("Innovate Labs")
                .industry("Fintech")
                .companySize("11-50")
                .location("Bengaluru, India")
                .website("https://innovatelabs.example.com")
                .build();

        // 1. Create Company
        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Innovate Labs"))
                .andExpect(jsonPath("$.data.verificationStatus").value("PENDING"));

        // 2. Get My Company
        mockMvc.perform(get("/api/v1/companies/my-company")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Innovate Labs"));

        // 3. Update My Company
        CompanyUpdateRequest updateReq = CompanyUpdateRequest.builder()
                .industry("Artificial Intelligence")
                .location("Hyderabad, India")
                .companySize("51-200")
                .build();

        mockMvc.perform(put("/api/v1/companies/my-company")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.industry").value("Artificial Intelligence"))
                .andExpect(jsonPath("$.data.location").value("Hyderabad, India"));

        // 4. Pending Company does NOT appear in public verified directory
        mockMvc.perform(get("/api/v1/companies?search=Innovate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        // 5. Admin verifies company -> now appears in public verified directory
        Company createdCompany = companyRepository.findByNameIgnoreCase("Innovate Labs").orElseThrow();
        createdCompany.setVerificationStatus(com.careerforge.entity.enums.CompanyVerificationStatus.VERIFIED);
        companyRepository.save(createdCompany);

        mockMvc.perform(get("/api/v1/companies?search=Innovate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Student cannot create company (403 Forbidden)")
    void testStudentCannotCreateCompany() throws Exception {
        CompanyCreateRequest createReq = CompanyCreateRequest.builder()
                .name("Student Company")
                .industry("EdTech")
                .build();

        mockMvc.perform(post("/api/v1/companies")
                        .header("Authorization", studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden());
    }
}
