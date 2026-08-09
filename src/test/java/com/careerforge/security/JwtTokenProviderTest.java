package com.careerforge.security;

import com.careerforge.config.JwtConfigProperties;
import com.careerforge.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtConfigProperties jwtConfigProperties;

    @BeforeEach
    void setUp() {
        jwtConfigProperties = new JwtConfigProperties();
        jwtConfigProperties.setSecret("c3VwZXItc2VjcmV0LWtleS1mb3ItY2FyZWVyZm9yZ2UtYXBwbGljYXRpb24tcHJvZHVjdGlvbi1yZWFkeQ==");
        jwtConfigProperties.setExpirationMs(3600000); // 1 hour
        jwtConfigProperties.setRefreshExpirationMs(86400000); // 24 hours

        jwtTokenProvider = new JwtTokenProvider(jwtConfigProperties);
    }

    @Test
    void testGenerateAndValidateAccessToken() {
        UserPrincipal principal = UserPrincipal.builder()
                .id(1L)
                .email("test@careerforge.local")
                .password("encoded_pass")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(Role.ROLE_STUDENT.name())))
                .enabled(true)
                .build();

        String token = jwtTokenProvider.generateAccessToken(principal);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("test@careerforge.local", jwtTokenProvider.getEmailFromToken(token));
    }

    @Test
    void testInvalidTokenValidation() {
        String invalidToken = "invalid.jwt.token";
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }
}
