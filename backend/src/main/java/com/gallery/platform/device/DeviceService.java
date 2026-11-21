package com.gallery.platform.device;

import com.gallery.platform.admin.Admin;
import com.gallery.platform.admin.AdminRepository;
import com.gallery.platform.audit.AuditService;
import com.gallery.platform.auth.RefreshToken;
import com.gallery.platform.auth.RefreshTokenRepository;
import com.gallery.platform.common.ApiException;
import com.gallery.platform.device.dto.DeviceDtos;
import com.gallery.platform.security.AppPrincipal;
import com.gallery.platform.security.JwtService;
import com.gallery.platform.security.TokenHash;
import com.gallery.platform.websocket.RealtimeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;

@Service
public class DeviceService {
    private final DeviceRepository deviceRepository;
    private final AdminRepository adminRepository;
    private final DevicePermissionRepository permissionRepository;
    private final DeviceSessionRepository sessionRepository;
    private final RemoteAccessConsentRepository consentRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final RealtimeService realtimeService;
    private final long refreshDays;
    private final long deviceAccessMinutes;
    private final SecureRandom random = new SecureRandom();

    public DeviceService(DeviceRepository deviceRepository,
                         AdminRepository adminRepository,
                         DevicePermissionRepository permissionRepository,
                         DeviceSessionRepository sessionRepository,
                         RemoteAccessConsentRepository consentRepository,
                         RefreshTokenRepository refreshTokenRepository,
                         PasswordEncoder passwordEncoder,
                         JwtService jwtService,
                         AuditService auditService,
                         RealtimeService realtimeService,
                         @Value("${app.security.jwt.refresh-token-days:14}") long refreshDays,
                         @Value("${app.security.jwt.device-access-token-minutes:60}") long deviceAccessMinutes) {
        this.deviceRepository = deviceRepository;
        this.adminRepository = adminRepository;
        this.permissionRepository = permissionRepository;
        this.sessionRepository = sessionRepository;
        this.consentRepository = consentRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.realtimeService = realtimeService;
        this.refreshDays = refreshDays;
        this.deviceAccessMinutes = deviceAccessMinutes;
    }

    // Admin operations
    @Transactional(readOnly = true)
    public List<DeviceDtos.DeviceView> listForAdmin(AppPrincipal adminPrincipal, String search, String status, Boolean revoked) {
        List<Device> devices = deviceRepository.findByOwnerAdminId(adminPrincipal.getId());
        return devices.stream()
                .filter(d -> !StringUtils.hasText(search) || containsIgnoreCase(d.getName(), search) || containsIgnoreCase(d.getDeviceUuid(), search))
                .filter(d -> !StringUtils.hasText(status) || status.equalsIgnoreCase(d.getStatus()))
                .filter(d -> revoked == null || d.isRevoked() == revoked)
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public DeviceDtos.DeviceView getForAdmin(AppPrincipal adminPrincipal, Long deviceId) {
        return toView(getOwnedDevice(adminPrincipal, deviceId));
    }

    @Transactional
    public DeviceDtos.DeviceView adminCreate(AppPrincipal adminPrincipal, DeviceDtos.AdminCreateDeviceRequest req, HttpServletRequest request) {
        if (deviceRepository.findByDeviceUuid(req.deviceUuid()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Device UUID already exists");
        }
        Admin admin = adminRepository.findById(adminPrincipal.getId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Admin not found"));
        Device d = new Device();
        d.setDeviceUuid(req.deviceUuid().trim());
        d.setName(req.name().trim());
        d.setDeviceType(req.deviceType());
        d.setOwnerAdmin(admin);
        d.setStatus("OFFLINE");
        d = deviceRepository.save(d);
        ensurePermission(d);
        auditService.logAdmin(admin.getId(), "DEVICE_CREATED", "DEVICE", String.valueOf(d.getId()), "Device created by admin", ip(request), true, d.getId());
        return toView(d);
    }

    @Transactional
    public DeviceDtos.DeviceView adminUpdate(AppPrincipal adminPrincipal, Long deviceId, DeviceDtos.AdminUpdateDeviceRequest req, HttpServletRequest request) {
        Device d = getOwnedDevice(adminPrincipal, deviceId);
        if (StringUtils.hasText(req.name())) d.setName(req.name().trim());
        if (req.deviceType() != null) d.setDeviceType(req.deviceType());
        d = deviceRepository.save(d);
        auditService.logAdmin(adminPrincipal.getId(), "DEVICE_UPDATED", "DEVICE", String.valueOf(d.getId()), "Device updated", ip(request), true, d.getId());
        return toView(d);
    }

    @Transactional
    public DeviceDtos.DeviceView revoke(AppPrincipal adminPrincipal, Long deviceId, boolean revoked, HttpServletRequest request) {
        Device d = getOwnedDevice(adminPrincipal, deviceId);
        d.setRevoked(revoked);
        d.setRevokedAt(revoked ? Instant.now() : null);
        if (revoked) d.setStatus("REVOKED");
        deviceRepository.save(d);
        auditService.logAdmin(adminPrincipal.getId(), revoked ? "DEVICE_REVOKED" : "DEVICE_UNREVOKED", "DEVICE", String.valueOf(d.getId()),
                revoked ? "Device revoked" : "Device unrevoked", ip(request), true, d.getId());
        realtimeService.deviceStatusChanged(d.getId(), d.getStatus(), revoked ? "revoked" : "unrevoked");
        return toView(d);
    }

    @Transactional
    public DeviceDtos.DeviceView updatePermissions(AppPrincipal adminPrincipal, Long deviceId, DeviceDtos.PermissionUpdateRequest req, HttpServletRequest request) {
        Device d = getOwnedDevice(adminPrincipal, deviceId);
        DevicePermission p = permissionRepository.findByDeviceId(d.getId()).orElseGet(() -> {
            DevicePermission np = new DevicePermission();
            np.setDevice(d);
            return np;
        });
        p.setCanView(req.canView());
        p.setCanStream(req.canStream());
        p.setCanEdit(req.canEdit());
        p.setCanDelete(req.canDelete());
        p.setCanSyncMetadata(req.canSyncMetadata());
        permissionRepository.save(p);
        auditService.logAdmin(adminPrincipal.getId(), "DEVICE_PERMISSIONS_UPDATED", "DEVICE_PERMISSION", String.valueOf(d.getId()),
                "Permissions updated", ip(request), true, d.getId());
        return toView(d);
    }

    @Transactional
    public DeviceDtos.DeviceView setConsent(AppPrincipal adminPrincipal, Long deviceId, DeviceDtos.ConsentRequest req, HttpServletRequest request) {
        Device d = getOwnedDevice(adminPrincipal, deviceId);
        Admin admin = adminRepository.findById(adminPrincipal.getId()).orElseThrow();
        RemoteAccessConsent c = new RemoteAccessConsent();
        c.setDevice(d);
        c.setAdmin(admin);
        c.setGranted(Boolean.TRUE.equals(req.granted()));
        c.setScope(req.scope());
        c.setExpiresAt(req.expiresAt());
        c.setNotes(req.notes());
        if (c.isGranted()) c.setGrantedAt(Instant.now()); else c.setRevokedAt(Instant.now());
        consentRepository.save(c);
        auditService.logAdmin(adminPrincipal.getId(), c.isGranted() ? "CONSENT_GRANTED" : "CONSENT_REVOKED", "REMOTE_ACCESS_CONSENT",
                String.valueOf(c.getId()), "Remote access consent updated", ip(request), true, d.getId());
        return toView(d);
    }

    // Device-side
    @Transactional
    public DeviceDtos.DeviceAuthResponse deviceRegister(DeviceDtos.DeviceRegisterRequest req, HttpServletRequest request) {
        // Registration requires owner context in this implementation; for simplicity accept ownerEmail in user-agent-free way via header X-Owner-Email or fallback: not provided.
        String ownerEmail = request != null ? request.getHeader("X-Owner-Email") : null;
        if (!StringUtils.hasText(ownerEmail)) {
            // allow registration only when device already exists and secret matches (re-registration) OR when a bootstrap single-admin exists
            // to keep API usable, try first admin as owner if exactly one exists
            List<Admin> admins = adminRepository.findAll();
            if (admins.size() == 1) ownerEmail = admins.get(0).getEmail();
            else throw new ApiException(HttpStatus.BAD_REQUEST, "ownerEmail required (X-Owner-Email header) for device registration");
        }
        Admin owner = adminRepository.findByEmailIgnoreCase(ownerEmail.trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Owner admin not found"));
        Device device = deviceRepository.findByDeviceUuid(req.deviceUuid().trim()).orElse(null);
        if (device == null) {
            device = new Device();
            device.setDeviceUuid(req.deviceUuid().trim());
            device.setOwnerAdmin(owner);
            device.setName(req.name().trim());
            device.setDeviceType(req.deviceType());
            device.setStatus("OFFLINE");
            if (StringUtils.hasText(req.deviceSecret())) {
                device.setDeviceSecretHash(passwordEncoder.encode(req.deviceSecret()));
            }
            device = deviceRepository.save(device);
            ensurePermission(device);
            auditService.logDevice(device.getId(), "DEVICE_REGISTERED", "DEVICE", String.valueOf(device.getId()), "Device registered", ip(request), true);
        } else {
            if (device.isRevoked()) throw new ApiException(HttpStatus.FORBIDDEN, "Device is revoked");
            if (!device.getOwnerAdmin().getId().equals(owner.getId())) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Device belongs to another owner");
            }
            // if secret hash exists, require match to update sensitive fields
            if (StringUtils.hasText(device.getDeviceSecretHash())) {
                if (!StringUtils.hasText(req.deviceSecret()) || !passwordEncoder.matches(req.deviceSecret(), device.getDeviceSecretHash())) {
                    throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid device credentials");
                }
            } else if (StringUtils.hasText(req.deviceSecret())) {
                device.setDeviceSecretHash(passwordEncoder.encode(req.deviceSecret()));
            }
            device.setName(req.name().trim());
            if (req.deviceType() != null) device.setDeviceType(req.deviceType());
            device.setAppVersion(req.appVersion());
            deviceRepository.save(device);
            auditService.logDevice(device.getId(), "DEVICE_REREGISTERED", "DEVICE", String.valueOf(device.getId()), "Device registration updated", ip(request), true);
        }
        return issueDeviceTokens(device, request);
    }

    @Transactional
    public DeviceDtos.DeviceAuthResponse deviceAuth(DeviceDtos.DeviceAuthRequest req, HttpServletRequest request) {
        Device device = deviceRepository.findByDeviceUuid(req.deviceUuid().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid device credentials"));
        if (device.isRevoked()) throw new ApiException(HttpStatus.FORBIDDEN, "Device is revoked");
        if (!StringUtils.hasText(device.getDeviceSecretHash()) || !passwordEncoder.matches(req.deviceSecret(), device.getDeviceSecretHash())) {
            auditService.logDevice(device.getId(), "DEVICE_AUTH_FAILED", "DEVICE", String.valueOf(device.getId()), "Failed device auth", ip(request), false);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid device credentials");
        }
        device.setLastSeenAt(Instant.now());
        device.setLastKnownIp(ip(request));
        device.setStatus("ONLINE");
        deviceRepository.save(device);
        auditService.logDevice(device.getId(), "DEVICE_AUTH", "DEVICE", String.valueOf(device.getId()), "Device authenticated", ip(request), true);
        realtimeService.deviceStatusChanged(device.getId(), "ONLINE", "authenticated");
        return issueDeviceTokens(device, request);
    }

    @Transactional
    public void heartbeat(AppPrincipal principal, DeviceDtos.HeartbeatRequest req, HttpServletRequest request) {
        Device d = deviceRepository.findById(principal.getDeviceId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Device not found"));
        if (d.isRevoked()) throw new ApiException(HttpStatus.FORBIDDEN, "Device revoked");
        d.setLastSeenAt(Instant.now());
        d.setLastKnownIp(ip(request));
        if (!"ONLINE".equals(d.getStatus())) d.setStatus("ONLINE");
        if (req != null && StringUtils.hasText(req.appVersion())) d.setAppVersion(req.appVersion());
        deviceRepository.save(d);
    }

    @Transactional
    public void updateStatus(AppPrincipal principal, DeviceDtos.DeviceStatusRequest req, HttpServletRequest request) {
        Device d = deviceRepository.findById(principal.getDeviceId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Device not found"));
        String status = req.status().toUpperCase();
        if (!List.of("ONLINE", "OFFLINE", "AWAY").contains(status)) throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status");
        d.setStatus(status);
        d.setLastSeenAt(Instant.now());
        deviceRepository.save(d);
        auditService.logDevice(d.getId(), "DEVICE_STATUS_CHANGED", "DEVICE", String.valueOf(d.getId()), "Status: " + status, ip(request), true);
        realtimeService.deviceStatusChanged(d.getId(), status, "reported");
    }

    public Device getOwnedDevice(AppPrincipal adminPrincipal, Long deviceId) {
        return deviceRepository.findByIdAndOwnerAdminId(deviceId, adminPrincipal.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Device not found"));
    }

    public Device getActiveDeviceForPrincipal(AppPrincipal principal) {
        Device d = deviceRepository.findById(principal.getDeviceId()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Device not found"));
        if (d.isRevoked()) throw new ApiException(HttpStatus.FORBIDDEN, "Device revoked");
        return d;
    }

    private void ensurePermission(Device d) {
        if (permissionRepository.findByDeviceId(d.getId()).isEmpty()) {
            DevicePermission p = new DevicePermission();
            p.setDevice(d);
            permissionRepository.save(p);
        }
    }

    private DeviceDtos.DeviceAuthResponse issueDeviceTokens(Device device, HttpServletRequest request) {
        String access = jwtService.generateDeviceAccessToken(device.getId(), device.getDeviceUuid());
        String refreshRaw = generateOpaqueToken();
        RefreshToken rt = new RefreshToken();
        rt.setSubjectType("DEVICE");
        rt.setSubjectId(String.valueOf(device.getId()));
        rt.setTokenHash(TokenHash.sha256(refreshRaw));
        rt.setExpiresAt(Instant.now().plus(refreshDays, ChronoUnit.DAYS));
        rt.setRevoked(false);
        rt.setIpAddress(ip(request));
        rt.setUserAgent(request != null ? truncate(request.getHeader("User-Agent"), 255) : null);
        refreshTokenRepository.save(rt);
        return new DeviceDtos.DeviceAuthResponse(access, refreshRaw, "Bearer", deviceAccessMinutes * 60, toView(device));
    }

    public DeviceDtos.DeviceView toView(Device d) {
        DevicePermission p = permissionRepository.findByDeviceId(d.getId()).orElse(null);
        DeviceDtos.PermissionView pv = p == null ? new DeviceDtos.PermissionView(true, false, false, false, true)
                : new DeviceDtos.PermissionView(p.isCanView(), p.isCanStream(), p.isCanEdit(), p.isCanDelete(), p.isCanSyncMetadata());
        RemoteAccessConsent consent = consentRepository.findTopByDeviceIdAndAdminIdOrderByCreatedAtDesc(d.getId(), d.getOwnerAdmin().getId()).orElse(null);
        DeviceDtos.ConsentView cv = consent == null ? null : new DeviceDtos.ConsentView(consent.getId(), consent.isGranted(), consent.getScope(), consent.getGrantedAt(), consent.getRevokedAt(), consent.getExpiresAt());
        return new DeviceDtos.DeviceView(d.getId(), d.getDeviceUuid(), d.getName(), d.getDeviceType(), d.getStatus(), d.isRevoked(),
                d.getLastSeenAt(), d.getLastKnownIp(), d.getAppVersion(), d.getCreatedAt(), pv, cv);
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean containsIgnoreCase(String value, String search) {
        return value != null && value.toLowerCase().contains(search.toLowerCase());
    }
    private String ip(HttpServletRequest request) {
        if (request == null) return null;
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return request.getRemoteAddr();
    }
    private String truncate(String s, int max) { if (s == null) return null; return s.length() <= max ? s : s.substring(0, max); }
}
