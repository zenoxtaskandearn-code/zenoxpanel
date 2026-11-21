package com.gallery.platform.admin;

import com.gallery.platform.audit.AuditLogRepository;
import com.gallery.platform.audit.dto.AuditDtos;
import com.gallery.platform.device.DeviceRepository;
import com.gallery.platform.media.MediaMetadataRepository;
import com.gallery.platform.security.AppPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {
    private final DeviceRepository deviceRepository;
    private final MediaMetadataRepository mediaRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardController(DeviceRepository deviceRepository, MediaMetadataRepository mediaRepository, AuditLogRepository auditLogRepository) {
        this.deviceRepository = deviceRepository;
        this.mediaRepository = mediaRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public Map<String, Object> dashboard(@AuthenticationPrincipal AppPrincipal principal) {
        Long adminId = principal.getId();
        long totalDevices = deviceRepository.countByOwnerAdminId(adminId);
        long onlineDevices = deviceRepository.countByOwnerAdminIdAndStatus(adminId, "ONLINE");
        long offlineDevices = deviceRepository.countByOwnerAdminIdAndStatus(adminId, "OFFLINE");
        long revokedDevices = deviceRepository.findByOwnerAdminId(adminId).stream().filter(d -> d.isRevoked()).count();
        long totalImages = mediaRepository.countByAdminAndType(adminId, "IMAGE");
        long totalVideos = mediaRepository.countByAdminAndType(adminId, "VIDEO");

        var recent = auditLogRepository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(l -> new AuditDtos.AuditLogView(
                        l.getId(), l.getActorType(), l.getActorId(),
                        l.getDevice() != null ? l.getDevice().getId() : null,
                        l.getAction(), l.getResourceType(), l.getResourceId(),
                        l.getDetails(), l.getIpAddress(), l.isSuccess(), l.getCreatedAt()))
                .getContent();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDevices", totalDevices);
        stats.put("onlineDevices", onlineDevices);
        stats.put("offlineDevices", offlineDevices);
        stats.put("revokedDevices", revokedDevices);
        stats.put("totalIndexedImages", totalImages);
        stats.put("totalIndexedVideos", totalVideos);
        stats.put("recentActivity", recent);
        stats.put("deviceConnectionStatistics", Map.of(
                "online", onlineDevices,
                "offline", offlineDevices,
                "total", totalDevices
        ));
        // All stats are derived from real database records (devices/media/audit). No fake data.
        return stats;
    }
}
