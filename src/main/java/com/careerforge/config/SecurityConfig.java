package com.careerforge.config;

import com.careerforge.security.CustomUserDetailsService;
import com.careerforge.security.JwtAuthenticationEntryPoint;
import com.careerforge.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /*
     * Reads the allowed frontend origins from the environment variable:
     *
     * CORS_ALLOWED_ORIGINS
     *
     * Example:
     * https://career-forge-coral.vercel.app
     *
     * Multiple origins can be separated by commas:
     *
     * https://career-forge-coral.vercel.app,https://www.example.com
     */
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173}")
    private String corsAllowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider();

        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {

        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                .csrf(AbstractHttpConfigurer::disable)

                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(unauthorizedHandler)
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .headers(headers ->
                        headers.frameOptions(
                                HeadersConfigurer.FrameOptionsConfig::sameOrigin
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        /*
                         * Public endpoints
                         */
                        .requestMatchers(
                                "/api/v1/health",
                                "/api/v1/auth/**",
                                "/h2-console/**"
                        ).permitAll()

                        /*
                         * Jobs are publicly accessible
                         */
                        .requestMatchers(
                                "/api/v1/jobs/**"
                        ).permitAll()

                        /*
                         * Public company endpoints
                         */
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/companies",
                                "/api/v1/companies/{id:[0-9]+}",
                                "/api/v1/companies/slug/**"
                        ).permitAll()

                        /*
                         * Everything else requires authentication
                         */
                        .anyRequest().authenticated()
                );

        http.authenticationProvider(authenticationProvider());

        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        /*
         * IMPORTANT:
         *
         * Do NOT use:
         *
         * setAllowedOriginPatterns(List.of("*"))
         *
         * because that allows every website.
         *
         * Instead, read the approved origins from:
         *
         * CORS_ALLOWED_ORIGINS
         */
        List<String> allowedOrigins = Arrays.stream(
                        corsAllowedOrigins.split(",")
                )
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        configuration.setAllowedOrigins(allowedOrigins);

        /*
         * HTTP methods allowed from the approved frontend.
         */
        configuration.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        /*
         * HTTP headers the frontend is allowed to send.
         */
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With"
        ));

        /*
         * Headers the browser is allowed to expose
         * to the frontend.
         */
        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));

        /*
         * Required because the application uses
         * authenticated requests / JWT-related credentials.
         */
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}