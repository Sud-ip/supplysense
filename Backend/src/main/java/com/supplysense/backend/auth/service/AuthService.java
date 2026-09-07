package com.supplysense.backend.auth.service;

import com.supplysense.backend.auth.domain.Role;
import com.supplysense.backend.auth.domain.Tenant;
import com.supplysense.backend.auth.domain.User;
import com.supplysense.backend.auth.dto.AuthResponse;
import com.supplysense.backend.auth.dto.LoginRequest;
import com.supplysense.backend.auth.dto.RegisterRequest;
import com.supplysense.backend.auth.repository.TenantRepository;
import com.supplysense.backend.auth.repository.UserRepository;
import com.supplysense.backend.common.exception.DuplicateEmailException;
import com.supplysense.backend.common.exception.InvalidCredentialsException;
import com.supplysense.backend.common.exception.InvalidTokenException;
import com.supplysense.backend.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Creates a new Tenant plus its first user as OWNER, in one transaction -
     * a tenant can never exist without an owner, and vice versa.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        Tenant tenant = tenantRepository.save(new Tenant(request.businessName()));

        User owner = new User(
                tenant,
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                Role.OWNER
        );
        userRepository.save(owner);

        return issueTokens(owner);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .filter(User::isActive)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return issueTokens(user);
    }

    public AuthResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(refreshToken);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Refresh token is invalid or expired");
        }

        if (!jwtService.isRefreshToken(claims)) {
            throw new InvalidTokenException("Token is not a refresh token");
        }

        UUID userId = jwtService.extractUserId(claims);
        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new InvalidTokenException("User no longer exists or is inactive"));

        return issueTokens(user);
    }

    private AuthResponse issueTokens(User user) {
        UUID tenantId = user.getTenant().getId();
        String accessToken = jwtService.generateAccessToken(
                user.getId(), tenantId, user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.accessTokenExpirationSeconds(),
                user.getId(),
                tenantId,
                user.getEmail(),
                user.getRole()
        );
    }
}
