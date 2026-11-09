package com.muntinlupa.barangay_ease.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization = request.getHeader("Authorization");

        // Walang Bearer token: ipagpatuloy ang security chain.
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(7).trim();

        if (token.isBlank()) {
            writeUnauthorized(response);
            return;
        }

        final AppUser user;

        // JWT parsing lamang ang nasa try-catch.
        try {
            user = jwtService.parse(token);

        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(response);
            return;
        }

        // Gumawa ng authenticated principal.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_" + user.role()
                                )
                        )
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        // IMPORTANT:
        // Nasa labas na ng JWT try-catch ang filter chain.
        // Hindi na mamamarkahang invalid JWT ang downstream errors.
        filterChain.doFilter(request, response);
    }

    private void writeUnauthorized(
            HttpServletResponse response
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"message\":\"Invalid or expired authentication token.\"}"
        );
    }
}