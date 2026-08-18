package com.careerforge.controller;

import com.careerforge.dto.request.RecruiterProfileRequest;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.Role;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecruiterControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String recruiterToken;
    private String studentToken;
    private User recruiterUser;

    @BeforeEach
    void setUp() {
        recruiterUser = userRepository.findByEmail("recruiter_prof_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("recruiter_prof_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_RECRUITER)
                        .enabled(true)
                        .build()));

        User studentUser = userRepository.findByEmail("student_rec_test@careerforge.local")
                .orElseGet(() -> userRepository.save(User.builder()
                        .email("student_rec_test@careerforge.local")
                        .passwordHash(passwordEncoder.encode("TestPass123!"))
                        .role(Role.ROLE_STUDENT)
                        .enabled(true)
                        .build()));

        recruiterToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(recruiterUser));
        studentToken = "Bearer " + jwtTokenProvider.generateAccessToken(UserPrincipal.create(studentUser));
    }

    @Test
    @DisplayName("Recruiter can create, retrieve, and update their profile")
    void testRecruiterProfileLifecycle() throws Exception {
        RecruiterProfileRequest request = RecruiterProfileRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .phone("+91-9876543210")
                .designation("Senior Talent Partner")
                .department("Engineering Hiring")
                .build();

        // 1. Create Profile
        mockMvc.perform(post("/api/v1/recruiters/profile")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("Jane"))
                .andExpect(jsonPath("$.data.designation").value("Senior Talent Partner"));

        // 2. Retrieve Profile
        mockMvc.perform(get("/api/v1/recruiters/profile")
                        .header("Authorization", recruiterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lastName").value("Doe"));

        // 3. Update Profile
        request.setDesignation("Lead Talent Partner");
        mockMvc.perform(put("/api/v1/recruiters/profile")
                        .header("Authorization", recruiterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.designation").value("Lead Talent Partner"));
    }

    @Test
    @DisplayName("Student access to Recruiter Profile should return 403 Forbidden")
    void testStudentAccessToRecruiterProfile_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/recruiters/profile")
                        .header("Authorization", studentToken))
                .andExpect(status().isForbidden());
    }
}
