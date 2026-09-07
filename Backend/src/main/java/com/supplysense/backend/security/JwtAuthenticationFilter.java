package com.supplysense.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Runs once per request. Validates the Bearer token (if present), and on
 * success populates both:
 *  - Spring Security's context (so @PreAuthorize / hasRole checks work)
 *  - TenantContext (so repository-level tenant filtering works from M2 on)
 *
 * Requests with no/invalid token simply proceed unauthenticated - it's
 * SecurityConfig's job to decide which endpoints require authentication.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                Claims claims = jwtService.parseAndValidate(token);

                if (!jwtService.isRefreshToken(claims)) {
                    UUID userId = jwtService.extractUserId(claims);
                    UUID tenantId = jwtService.extractTenantId(claims);
                    String email = jwtService.extractEmail(claims);
                    String role = jwtService.extractRole(claims);

                    TenantContext.set(new TenantContext.AuthenticatedPrincipal(userId, tenantId, email, role));

                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
                // A refresh token presented as a Bearer access token is
                // silently ignored here - it simply won't authenticate,
                // which is the correct behavior (refresh tokens are only
                // valid at POST /api/v1/auth/refresh).

            } catch (JwtException | IllegalArgumentException ex) {
                // Invalid/expired token: leave unauthenticated, let
                // SecurityConfig's entry point return 401 for protected
                // routes. We don't throw here - that's this filter's job
                // only for validation, not for deciding access.
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
