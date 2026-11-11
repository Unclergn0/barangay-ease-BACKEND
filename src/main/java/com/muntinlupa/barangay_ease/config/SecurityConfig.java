package com.muntinlupa.barangay_ease.config;

import com.muntinlupa.barangay_ease.security.JwtAuthenticationFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
import java.util.ArrayList;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            @Value("${app.cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500,https://barangayease.netlify.app}")
            String origins
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;

        List<String> mutableOrigins = new ArrayList<>(Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .distinct()
                .toList());

        // Siguraduhing laging kasama ang Netlify frontend domain
        if (!mutableOrigins.contains("https://barangayease.netlify.app")) {
            mutableOrigins.add("https://barangayease.netlify.app");
        }

        this.allowedOrigins = mutableOrigins;

        if (this.allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one CORS allowed origin must be configured."
            );
        }
    }

    // ==========================================
    // PASSWORD ENCODER
    // ==========================================

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    // ==========================================
    // SECURITY FILTER CHAIN
    // ==========================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            // CORS
            .cors(cors -> cors
                    .configurationSource(corsConfigurationSource())
            )

            // JWT stateless authentication
            .csrf(csrf -> csrf.disable())

            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())

            .sessionManagement(session -> session
                    .sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            // ==================================
            // AUTHORIZATION
            // ==================================

            .authorizeHttpRequests(auth -> auth

                // Allow browser preflight requests
                .requestMatchers(
                        HttpMethod.OPTIONS,
                        "/**"
                ).permitAll()

                // ----------------------------------
                // PUBLIC ADMIN AUTHENTICATION
                // ----------------------------------

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/admins/register",
                        "/api/admins/login"
                ).permitAll()

                .requestMatchers(
                        HttpMethod.PUT,
                        "/api/admins/update-password"
                ).permitAll()

                // ----------------------------------
                // PUBLIC RESIDENT AUTHENTICATION
                // ----------------------------------

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/auth/login",
                        "/api/residents/login",
                        "/api/residents/register",
                        "/api/residents/forgot-password"
                ).permitAll()

                // ----------------------------------
                // PUBLIC RESOURCES
                // ----------------------------------

                .requestMatchers(
                        "/error",
                        "/static/**"
                ).permitAll()

                // ----------------------------------
                // ADMIN-ONLY ENDPOINTS
                // ----------------------------------

                .requestMatchers(
                        "/api/services/admin/**",
                        "/api/complaints/admin/**"
                ).hasRole("ADMIN")

                .requestMatchers(
                        HttpMethod.POST,
                        "/api/notifications"
                ).hasRole("ADMIN")

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/residents"
                ).hasRole("ADMIN")

                // ----------------------------------
                // RESIDENT-ONLY ENDPOINTS
                // ----------------------------------

                .requestMatchers(
                        "/api/ai/**",
                        "/api/resident/**",
                        "/api/feedback"
                ).hasRole("RESIDENT")

                .requestMatchers(
                        "/api/residents/**"
                ).hasRole("RESIDENT")

                // ----------------------------------
                // ALL OTHER ENDPOINTS
                // REQUIRE AUTHENTICATION
                // ----------------------------------

                .anyRequest().authenticated()
            );

        // ==================================
        // JWT FILTER
        // ==================================

        http.addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterServletRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    // ==========================================
    // CORS CONFIGURATION
    // ==========================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(allowedOrigins);

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept"
        ));

        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));

        configuration.setAllowCredentials(false);

        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}