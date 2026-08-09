package com.careerforge.service;

import com.careerforge.config.JwtConfigProperties;
import com.careerforge.entity.RefreshToken;
import com.careerforge.entity.User;
import com.careerforge.exception.TokenRefreshException;
import com.careerforge.repository.RefreshTokenRepository;
import com.careerforge.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtConfigProperties jwtConfigProperties;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        lenient().when(jwtConfigProperties.getRefreshExpirationMs()).thenReturn(604800000L);
    }

    @Test
    void testCreateRefreshToken() {
        User user = User.builder().id(1L).email("user@careerforge.local").build();

        when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

        RefreshToken token = refreshTokenService.createRefreshToken(user);

        assertNotNull(token);
        assertNotNull(token.getToken());
        assertEquals(user, token.getUser());
        assertFalse(token.isRevoked());
    }

    @Test
    void testExpiredTokenThrowsTokenRefreshException() {
        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired_token")
                .expiryDate(Instant.now().minusSeconds(3600))
                .revoked(false)
                .build();

        assertThrows(TokenRefreshException.class, () -> refreshTokenService.verifyExpiration(expiredToken));
        verify(refreshTokenRepository, times(1)).delete(expiredToken);
    }
}
