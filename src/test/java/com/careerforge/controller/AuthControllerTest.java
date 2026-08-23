package com.careerforge.controller;

import com.careerforge.dto.request.LoginRequest;
import com.careerforge.dto.request.RegisterRequest;
import com.careerforge.entity.enums.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testRegisterAndLoginIntegration() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("integrationtest@careerforge.local")
                .password("DevPass123!")
                .role(Role.ROLE_STUDENT)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.user.email").value("integrationtest@careerforge.local"));

        LoginRequest loginRequest = LoginRequest.builder()
                .email("integrationtest@careerforge.local")
                .password("DevPass123!")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    void testRegisterStudent_AutoProvisionsStudentProfile() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("autostudent@careerforge.local")
                .password("DevPass123!")
                .role(Role.ROLE_STUDENT)
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(responseJson).get("data").get("accessToken").asText();

        // Immediately fetch profile using token without manual profile creation
        mockMvc.perform(get("/api/v1/students/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("autostudent@careerforge.local"))
                .andExpect(jsonPath("$.data.firstName").value("Student"))
                .andExpect(jsonPath("$.data.lastName").value("User"));
    }

    @Test
    void testRegisterRecruiter_AutoProvisionsRecruiterProfile() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email("autorecruiter@careerforge.local")
                .password("DevPass123!")
                .role(Role.ROLE_RECRUITER)
                .build();

        String responseJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token = objectMapper.readTree(responseJson).get("data").get("accessToken").asText();

        // Immediately fetch profile using token without manual profile creation
        mockMvc.perform(get("/api/v1/recruiters/profile")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("autorecruiter@careerforge.local"))
                .andExpect(jsonPath("$.data.firstName").value("Recruiter"))
                .andExpect(jsonPath("$.data.lastName").value("User"));
    }
}
