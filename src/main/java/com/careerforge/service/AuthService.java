package com.careerforge.service;

import com.careerforge.dto.request.LoginRequest;
import com.careerforge.dto.request.RegisterRequest;
import com.careerforge.dto.request.TokenRefreshRequest;
import com.careerforge.dto.response.AuthResponse;
import com.careerforge.dto.response.TokenRefreshResponse;
import com.careerforge.dto.response.UserResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    TokenRefreshResponse refreshToken(TokenRefreshRequest request);

    void logout(Long userId);

    UserResponse getCurrentUser(String email);
}
