package com.marketflow.service;

import com.marketflow.dto.auth.*;
import com.marketflow.exception.DuplicateResourceException;
import com.marketflow.model.RefreshToken;
import com.marketflow.model.User;
import com.marketflow.model.enums.UserRole;
import com.marketflow.repository.RefreshTokenRepository;
import com.marketflow.repository.UserRepository;
import com.marketflow.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", email);
        }

        User user = new User(
                email,
                passwordEncoder.encode(request.getPassword()),
                request.getName().trim(),
                UserRole.ROLE_USER
        );
        user = userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String tokenHash = request.getRefreshToken().trim();
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadCredentialsException("Refresh token has expired. Please log in again.");
        }

        // Automatic Token Rotation & Theft Detection
        if (refreshToken.isRevoked()) {
            log.warn("Security Alert: Revoked refresh token reused for user [{}]. Revoking all active tokens!",
                    refreshToken.getUser().getEmail());
            refreshTokenRepository.deleteByUser(refreshToken.getUser());
            throw new BadCredentialsException("Security violation: Revoked refresh token detected.");
        }

        // Revoke the old token (Single-use rotation)
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Issue new token pair
        return issueTokens(refreshToken.getUser());
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        // Single device logout: cleanly removes the refresh token for this device session
        // Other devices retain their active sessions completely uninterrupted
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            refreshTokenRepository.findByTokenHash(request.getRefreshToken().trim())
                    .ifPresent(refreshTokenRepository::delete);
        }
    }

    @Transactional
    public void logoutAll(String email) {
        // Full account logout: terminates sessions across all devices
        userRepository.findByEmail(email.trim().toLowerCase())
                .ifPresent(refreshTokenRepository::deleteByUser);
    }

    @Transactional(readOnly = true)
    public UserSummaryDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return new UserSummaryDto(user.getId(), user.getEmail(), user.getName(), user.getRole().name());
    }

    private AuthResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshTokenString = jwtService.generateSecureRandomToken();

        // 7 days refresh token validity
        Instant expiry = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshToken refreshToken = new RefreshToken(refreshTokenString, user, expiry);
        refreshTokenRepository.save(refreshToken);

        UserSummaryDto userSummary = new UserSummaryDto(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );

        return new AuthResponse(accessToken, refreshTokenString, userSummary);
    }
}
