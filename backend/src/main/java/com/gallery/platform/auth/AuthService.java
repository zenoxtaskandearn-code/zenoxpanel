package com.gallery.platform.auth;

import com.gallery.platform.admin.Admin;
import com.gallery.platform.admin.AdminRepository;
import com.gallery.platform.audit.AuditService;
import com.gallery.platform.auth.dto.AuthDtos;
import com.gallery.platform.common.ApiException;
import com.gallery.platform.security.AppPrincipal;
import com.gallery.platform.security.JwtService;
import com.gallery.platform.security.TokenHash;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AuthService {
    private final AdminRepository adminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    private final boolean registrationEnabled;
    private final String inviteCode;
    private final long refreshDays;
    private final long accessMinutes;

    private final SecureRandom random = new SecureRandom();

    public AuthService(AdminRepository adminRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService,
                       @Value("${app.security.registration.enabled:true}") boolean registrationEnabled,
                       @Value("${app.security.registration.invite-code:}") String inviteCode,
                       @Value("${app.security.jwt.refresh-token-days:14}") long refreshDays,
                       @Value("${app.security.jwt.access-token-minutes:30}") long accessMinutes) {
        this.adminRepository = adminRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.registrationEnabled = registrationEnabled;
        this.inviteCode = inviteCode == null ? "" : inviteCode;
        this.refreshDays = refreshDays;
        this.accessMinutes = accessMinutes;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest req, HttpServletRequest request) {
        if (!registrationEnabled) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin registration is disabled");
        }
        if (!inviteCode.isBlank() && !inviteCode.equals(req.inviteCode())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invalid registration invite code");
        }
        if (adminRepository.existsByEmailIgnoreCase(req.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        Admin admin = new Admin();
        admin.setEmail(req.email().trim().toLowerCase());
        admin.setName(req.name().trim());
        admin.setPasswordHash(passwordEncoder.encode(req.password()));
        admin.setRole("ADMIN");
        admin.setEnabled(true);
        admin = adminRepository.save(admin);
        auditService.logAdmin(admin.getId(), "ADMIN_REGISTERED", "ADMIN", String.valueOf(admin.getId()),
                "Admin registered: " + admin.getEmail(), ip(request), true, null);
        return issueTokens(admin, request);
    }

    @Transactional
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest req, HttpServletRequest request) {
        Admin admin = adminRepository.findByEmailIgnoreCase(req.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!admin.isEnabled() || !passwordEncoder.matches(req.password(), admin.getPasswordHash())) {
            auditService.logAdmin(admin != null ? admin.getId() : null, "ADMIN_LOGIN_FAILED", "ADMIN",
                    req.email(), "Failed login", ip(request), false, null);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        admin.setLastLoginAt(Instant.now());
        adminRepository.save(admin);
        auditService.logAdmin(admin.getId(), "ADMIN_LOGIN", "ADMIN", String.valueOf(admin.getId()),
                "Admin login", ip(request), true, null);
        return issueTokens(admin, request);
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest req, HttpServletRequest request) {
        String hash = TokenHash.sha256(req.refreshToken());
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now()) || !"ADMIN".equals(token.getSubjectType())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }
        Admin admin = adminRepository.findById(Long.parseLong(token.getSubjectId()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        token.setRevoked(true);
        token.setRevokedAt(Instant.now());
        AuthDtos.AuthResponse response = issueTokens(admin, request);
        token.setReplacedByTokenHash(TokenHash.sha256(response.refreshToken()));
        refreshTokenRepository.save(token);
        return response;
    }

    @Transactional
    public void logout(AuthDtos.LogoutRequest req, AppPrincipal principal, HttpServletRequest request) {
        if (req != null && req.refreshToken() != null && !req.refreshToken().isBlank()) {
            String hash = TokenHash.sha256(req.refreshToken());
            refreshTokenRepository.findByTokenHash(hash).ifPresent(t -> {
                t.setRevoked(true);
                t.setRevokedAt(Instant.now());
                refreshTokenRepository.save(t);
            });
        }
        if (principal != null && principal.isAdmin()) {
            auditService.logAdmin(principal.getId(), "ADMIN_LOGOUT", "ADMIN", String.valueOf(principal.getId()),
                    "Admin logout", ip(request), true, null);
        }
    }

    @Transactional(readOnly = true)
    public AuthDtos.AdminProfile me(AppPrincipal principal) {
        Admin admin = adminRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admin not found"));
        return toProfile(admin);
    }

    @Transactional
    public AuthDtos.AdminProfile updateProfile(AppPrincipal principal, AuthDtos.UpdateProfileRequest req, HttpServletRequest request) {
        Admin admin = adminRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admin not found"));
        admin.setName(req.name().trim());
        adminRepository.save(admin);
        auditService.logAdmin(admin.getId(), "ADMIN_PROFILE_UPDATED", "ADMIN", String.valueOf(admin.getId()),
                "Profile updated", ip(request), true, null);
        return toProfile(admin);
    }

    @Transactional
    public void changePassword(AppPrincipal principal, AuthDtos.ChangePasswordRequest req, HttpServletRequest request) {
        Admin admin = adminRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admin not found"));
        if (!passwordEncoder.matches(req.currentPassword(), admin.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        admin.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        adminRepository.save(admin);
        auditService.logAdmin(admin.getId(), "ADMIN_PASSWORD_CHANGED", "ADMIN", String.valueOf(admin.getId()),
                "Password changed", ip(request), true, null);
    }

    private AuthDtos.AuthResponse issueTokens(Admin admin, HttpServletRequest request) {
        String access = jwtService.generateAdminAccessToken(admin.getId(), admin.getEmail(), admin.getRole());
        String refreshRaw = generateOpaqueToken();
        RefreshToken rt = new RefreshToken();
        rt.setSubjectType("ADMIN");
        rt.setSubjectId(String.valueOf(admin.getId()));
        rt.setTokenHash(TokenHash.sha256(refreshRaw));
        rt.setExpiresAt(Instant.now().plus(refreshDays, ChronoUnit.DAYS));
        rt.setRevoked(false);
        rt.setIpAddress(ip(request));
        rt.setUserAgent(request != null ? truncate(request.getHeader("User-Agent"), 255) : null);
        refreshTokenRepository.save(rt);
        return new AuthDtos.AuthResponse(access, refreshRaw, "Bearer", accessMinutes * 60, toProfile(admin));
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private AuthDtos.AdminProfile toProfile(Admin admin) {
        return new AuthDtos.AdminProfile(admin.getId(), admin.getEmail(), admin.getName(), admin.getRole());
    }

    private String ip(HttpServletRequest request) {
        if (request == null) return null;
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
