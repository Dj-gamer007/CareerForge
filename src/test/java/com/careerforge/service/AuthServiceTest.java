package com.careerforge.service;

import com.careerforge.dto.request.RegisterRequest;
import com.careerforge.dto.response.AuthResponse;
import com.careerforge.entity.RefreshToken;
import com.careerforge.entity.User;
import com.careerforge.entity.enums.Role;
import com.careerforge.exception.BadRequestException;
import com.careerforge.repository.UserRepository;
import com.careerforge.security.JwtTokenProvider;
import com.careerforge.security.UserPrincipal;
import com.careerforge.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void testRegisterUserSuccessfully() {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@careerforge.local")
                .password("Password123!")
                .role(Role.ROLE_STUDENT)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_pass");

        User savedUser = User.builder()
                .id(10L)
                .email("newuser@careerforge.local")
                .passwordHash("encoded_pass")
                .role(Role.ROLE_STUDENT)
                .enabled(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateAccessToken(any(UserPrincipal.class))).thenReturn("mock_access_token");
        when(refreshTokenService.createRefreshToken(any())).thenReturn(
                RefreshToken.builder().token("mock_refresh_token").build()
        );

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("mock_access_token", response.getAccessToken());
        assertEquals("mock_refresh_token", response.getRefreshToken());
        assertEquals("newuser@careerforge.local", response.getUser().getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsBadRequest() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@careerforge.local")
                .password("Password123!")
                .role(Role.ROLE_STUDENT)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }
}
