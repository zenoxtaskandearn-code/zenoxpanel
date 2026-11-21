package com.gallery.platform.operation;

import com.gallery.platform.admin.Admin;
import com.gallery.platform.admin.AdminRepository;
import com.gallery.platform.audit.AuditService;
import com.gallery.platform.common.ApiException;
import com.gallery.platform.device.Device;
import com.gallery.platform.device.DevicePermission;
import com.gallery.platform.device.DevicePermissionRepository;
import com.gallery.platform.device.DeviceService;
import com.gallery.platform.device.RemoteAccessConsentRepository;
import com.gallery.platform.media.MediaMetadata;
import com.gallery.platform.media.MediaMetadataRepository;
import com.gallery.platform.operation.dto.OperationDtos;
import com.gallery.platform.security.AppPrincipal;
import com.gallery.platform.websocket.RealtimeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class OperationService {
    private final MediaOperationRequestRepository repository;
    private final DeviceService deviceService;
    private final AdminRepository adminRepository;
    private final MediaMetadataRepository mediaRepository;
    private final DevicePermissionRepository permissionRepository;
    private final RemoteAccessConsentRepository consentRepository;
    private final AuditService auditService;
    private final RealtimeService realtimeService;
    private final int defaultExpiryMinutes;

    public OperationService(MediaOperationRequestRepository repository,
                            DeviceService deviceService,
                            AdminRepository adminRepository,
                            MediaMetadataRepository mediaRepository,
                            DevicePermissionRepository permissionRepository,
                            RemoteAccessConsentRepository consentRepository,
                            AuditService auditService,
                            RealtimeService realtimeService,
                            @Value("${app.operations.default-expiry-minutes:30}") int defaultExpiryMinutes) {
        this.repository = repository;
        this.deviceService = deviceService;
        this.adminRepository = adminRepository;
        this.mediaRepository = mediaRepository;
        this.permissionRepository = permissionRepository;
        this.consentRepository = consentRepository;
        this.auditService = auditService;
        this.realtimeService = realtimeService;
        this.defaultExpiryMinutes = defaultExpiryMinutes;
    }

    @Transactional
    public OperationDtos.OperationView create(AppPrincipal adminPrincipal, OperationDtos.CreateOperationRequest req, HttpServletRequest request) {
        Device device = deviceService.getOwnedDevice(adminPrincipal, req.deviceId());
        requireActiveConsent(device, adminPrincipal.getId());
        String op = req.operationType().toUpperCase();
        if (!List.of("PREVIEW", "EDIT", "DELETE").contains(op)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid operation type");
        }
        enforcePermission(device, op);
        MediaMetadata media = null;
        if (req.mediaId() != null) {
            media = mediaRepository.findByIdAndDeviceId(req.mediaId(), device.getId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found for device"));
        } else if (List.of("PREVIEW", "EDIT", "DELETE").contains(op)) {
            // allow device-level preview requests? keep flexible but usually media-specific
        }
        if ("DELETE".equals(op) && media == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "mediaId required for DELETE operation");
        }
        Admin admin = adminRepository.findById(adminPrincipal.getId()).orElseThrow();
        MediaOperationRequest r = new MediaOperationRequest();
        r.setDevice(device);
        r.setAdmin(admin);
        r.setMedia(media);
        r.setOperationType(op);
        r.setPayload(req.payload());
        r.setStatus("PENDING");
        r.setRequestedAt(Instant.now());
        int mins = req.expiresInMinutes() != null ? req.expiresInMinutes() : defaultExpiryMinutes;
        r.setExpiresAt(Instant.now().plus(mins, ChronoUnit.MINUTES));
        r = repository.save(r);
        auditService.logAdmin(admin.getId(), "REMOTE_OPERATION_REQUESTED", "MEDIA_OPERATION_REQUEST", String.valueOf(r.getId()),
                op + " requested for device " + device.getId(), ip(request), true, device.getId());
        realtimeService.operationUpdated(device.getId(), r.getId(), r.getStatus(), "Requested by admin");
        return toView(r);
    }

    @Transactional(readOnly = true)
    public Page<OperationDtos.OperationView> listForAdmin(AppPrincipal admin, Long deviceId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "requestedAt"));
        Page<MediaOperationRequest> p;
        if (deviceId != null) {
            deviceService.getOwnedDevice(admin, deviceId); // ownership check
            p = repository.findByDeviceId(deviceId, pr);
        } else {
            p = repository.findByAdminId(admin.getId(), pr);
        }
        return p.map(this::toView);
    }

    @Transactional(readOnly = true)
    public OperationDtos.OperationView getForAdmin(AppPrincipal admin, Long id) {
        MediaOperationRequest r = repository.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operation not found"));
        if (!r.getAdmin().getId().equals(admin.getId())) throw new ApiException(HttpStatus.NOT_FOUND, "Operation not found");
        return toView(r);
    }

    @Transactional(readOnly = true)
    public List<OperationDtos.OperationView> pendingForDevice(AppPrincipal devicePrincipal) {
        Device device = deviceService.getActiveDeviceForPrincipal(devicePrincipal);
        expireForDeviceIfNeeded(device.getId());
        return repository.findByDeviceIdAndStatusIn(device.getId(), List.of("PENDING", "ACKNOWLEDGED", "IN_PROGRESS"))
                .stream().map(this::toView).toList();
    }

    @Transactional
    public OperationDtos.OperationView ack(AppPrincipal devicePrincipal, Long operationId, OperationDtos.DeviceAckRequest req, HttpServletRequest request) {
        Device device = deviceService.getActiveDeviceForPrincipal(devicePrincipal);
        MediaOperationRequest r = repository.findByIdAndDeviceId(operationId, device.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operation not found"));
        ensureNotFinal(r);
        expireIfNeeded(r);
        if ("EXPIRED".equals(r.getStatus())) return toView(r);
        r.setStatus("ACKNOWLEDGED");
        r.setAcknowledgedAt(Instant.now());
        if (req != null) r.setDeviceAckMessage(req.message());
        repository.save(r);
        auditService.logDevice(device.getId(), "REMOTE_OPERATION_ACK", "MEDIA_OPERATION_REQUEST", String.valueOf(r.getId()),
                "Operation acknowledged", ip(request), true);
        realtimeService.operationUpdated(device.getId(), r.getId(), r.getStatus(), "Acknowledged by device");
        return toView(r);
    }

    @Transactional
    public OperationDtos.OperationView updateStatus(AppPrincipal devicePrincipal, Long operationId, OperationDtos.DeviceStatusUpdateRequest req, HttpServletRequest request) {
        Device device = deviceService.getActiveDeviceForPrincipal(devicePrincipal);
        MediaOperationRequest r = repository.findByIdAndDeviceId(operationId, device.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Operation not found"));
        ensureNotFinal(r);
        expireIfNeeded(r);
        if ("EXPIRED".equals(r.getStatus())) return toView(r);
        String status = req.status().toUpperCase();
        if (!List.of("ACKNOWLEDGED", "IN_PROGRESS", "COMPLETED", "FAILED").contains(status)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
        // Actual media operations must be authorized and executed by the Android application.
        // Backend only tracks requested/acknowledged/completed states reported by the device.
        r.setStatus(status);
        if ("ACKNOWLEDGED".equals(status) && r.getAcknowledgedAt() == null) r.setAcknowledgedAt(Instant.now());
        if ("COMPLETED".equals(status) || "FAILED".equals(status)) r.setCompletedAt(Instant.now());
        if (StringUtils.hasText(req.message())) r.setDeviceAckMessage(req.message());
        if ("FAILED".equals(status)) r.setFailureReason(StringUtils.hasText(req.failureReason()) ? req.failureReason() : req.message());
        repository.save(r);
        auditService.logDevice(device.getId(), "REMOTE_OPERATION_STATUS", "MEDIA_OPERATION_REQUEST", String.valueOf(r.getId()),
                "Status: " + status + (req.message() != null ? " - " + req.message() : ""), ip(request), !"FAILED".equals(status));
        realtimeService.operationUpdated(device.getId(), r.getId(), r.getStatus(), req.message());
        return toView(r);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expirePendingOperations() {
        List<MediaOperationRequest> expired = repository.findByStatusAndExpiresAtBefore("PENDING", Instant.now());
        for (MediaOperationRequest r : expired) {
            r.setStatus("EXPIRED");
            r.setCompletedAt(Instant.now());
            r.setFailureReason("Request expired before device acknowledgment");
            repository.save(r);
            auditService.logSystem("REMOTE_OPERATION_EXPIRED", "MEDIA_OPERATION_REQUEST", String.valueOf(r.getId()), "Expired", false, r.getDevice().getId());
            realtimeService.operationUpdated(r.getDevice().getId(), r.getId(), "EXPIRED", "Expired");
        }
        // also acknowledged/in_progress past expiry
        for (String st : List.of("ACKNOWLEDGED", "IN_PROGRESS")) {
            List<MediaOperationRequest> stale = repository.findByStatusAndExpiresAtBefore(st, Instant.now());
            for (MediaOperationRequest r : stale) {
                r.setStatus("EXPIRED");
                r.setCompletedAt(Instant.now());
                r.setFailureReason("Request expired before completion");
                repository.save(r);
                auditService.logSystem("REMOTE_OPERATION_EXPIRED", "MEDIA_OPERATION_REQUEST", String.valueOf(r.getId()), "Expired in state " + st, false, r.getDevice().getId());
                realtimeService.operationUpdated(r.getDevice().getId(), r.getId(), "EXPIRED", "Expired");
            }
        }
    }

    private void expireForDeviceIfNeeded(Long deviceId) {
        // opportunistic; scheduled job is primary
        repository.findByDeviceIdAndStatusIn(deviceId, List.of("PENDING", "ACKNOWLEDGED", "IN_PROGRESS"))
                .forEach(r -> { expireIfNeeded(r); if ("EXPIRED".equals(r.getStatus())) repository.save(r); });
    }

    private void expireIfNeeded(MediaOperationRequest r) {
        if (r.getExpiresAt() != null && r.getExpiresAt().isBefore(Instant.now())
                && List.of("PENDING", "ACKNOWLEDGED", "IN_PROGRESS").contains(r.getStatus())) {
            r.setStatus("EXPIRED");
            r.setCompletedAt(Instant.now());
            r.setFailureReason("Request expired");
        }
    }

    private void ensureNotFinal(MediaOperationRequest r) {
        if (List.of("COMPLETED", "FAILED", "EXPIRED", "CANCELLED").contains(r.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "Operation is already in a final state");
        }
    }

    private void requireActiveConsent(Device device, Long adminId) {
        if (device.isRevoked()) throw new ApiException(HttpStatus.FORBIDDEN, "Device is revoked");
        var c = consentRepository.findTopByDeviceIdAndAdminIdOrderByCreatedAtDesc(device.getId(), adminId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "Remote access consent not granted"));
        if (!c.isGranted()) throw new ApiException(HttpStatus.FORBIDDEN, "Remote access consent not granted");
        if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Remote access consent expired");
        }
    }

    private void enforcePermission(Device device, String op) {
        DevicePermission p = permissionRepository.findByDeviceId(device.getId()).orElse(null);
        if (p == null) return;
        if ("PREVIEW".equals(op) && !p.isCanView()) throw new ApiException(HttpStatus.FORBIDDEN, "View/preview not permitted");
        if ("EDIT".equals(op) && !p.isCanEdit()) throw new ApiException(HttpStatus.FORBIDDEN, "Edit not permitted");
        if ("DELETE".equals(op) && !p.isCanDelete()) throw new ApiException(HttpStatus.FORBIDDEN, "Delete not permitted");
    }

    private OperationDtos.OperationView toView(MediaOperationRequest r) {
        return new OperationDtos.OperationView(r.getId(), r.getDevice().getId(), r.getAdmin().getId(),
                r.getMedia() != null ? r.getMedia().getId() : null, r.getOperationType(), r.getPayload(), r.getStatus(),
                r.getFailureReason(), r.getRequestedAt(), r.getExpiresAt(), r.getAcknowledgedAt(), r.getCompletedAt(), r.getDeviceAckMessage());
    }

    private String ip(HttpServletRequest request) {
        if (request == null) return null;
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
