package com.supplysense.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
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
 * success populates THREE things:
 *  - Spring Security's context (so @PreAuthorize / hasRole checks work)
 *  - TenantContext (so repository-level tenant filtering works)
 *  - MDC (so every structured log line for this request carries
 *    tenantId/userId, added in M6 - see logback-spring.xml)
 *
 * Requests with no/invalid token simply proceed unauthenticated - it's
 * SecurityConfig's job to decide which endpoints require authentication.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TENANT_ID_MDC_KEY = "tenantId";
    private static final String USER_ID_MDC_KEY = "userId";

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
                    MDC.put(TENANT_ID_MDC_KEY, tenantId.toString());
                    MDC.put(USER_ID_MDC_KEY, userId.toString());

                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                    var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }

            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove(TENANT_ID_MDC_KEY);
            MDC.remove(USER_ID_MDC_KEY);
        }
    }
}
