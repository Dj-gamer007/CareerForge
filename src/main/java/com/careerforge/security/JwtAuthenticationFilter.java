package com.careerforge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                String email = jwtTokenProvider.getEmailFromToken(jwt);

                UserDetails userDetails = customUserDetailsService.loadUserByUsername(email);
                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    log.warn("User account is disabled for email: {}", email);
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
                    com.careerforge.dto.response.ErrorResponse errorResponse = com.careerforge.dto.response.ErrorResponse.builder()
                            .status(org.springframework.http.HttpStatus.FORBIDDEN.value())
                            .error(org.springframework.http.HttpStatus.FORBIDDEN.getReasonPhrase())
                            .code("ACCOUNT_DISABLED")
                            .message("Your account has been disabled by an administrator. Please contact support for assistance.")
                            .path(request.getRequestURI())
                            .timestamp(java.time.LocalDateTime.now())
                            .build();
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                    mapper.writeValue(response.getOutputStream(), errorResponse);
                    return;
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
