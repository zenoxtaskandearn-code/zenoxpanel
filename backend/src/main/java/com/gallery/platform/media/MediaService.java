package com.gallery.platform.media;

import com.gallery.platform.audit.AuditService;
import com.gallery.platform.common.ApiException;
import com.gallery.platform.device.Device;
import com.gallery.platform.device.DevicePermission;
import com.gallery.platform.device.DevicePermissionRepository;
import com.gallery.platform.device.DeviceService;
import com.gallery.platform.device.RemoteAccessConsentRepository;
import com.gallery.platform.media.dto.MediaDtos;
import com.gallery.platform.security.AppPrincipal;
import com.gallery.platform.websocket.RealtimeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class MediaService {
    private final MediaMetadataRepository mediaRepository;
    private final DeviceService deviceService;
    private final DevicePermissionRepository permissionRepository;
    private final RemoteAccessConsentRepository consentRepository;
    private final AuditService auditService;
    private final RealtimeService realtimeService;

    public MediaService(MediaMetadataRepository mediaRepository,
                        DeviceService deviceService,
                        DevicePermissionRepository permissionRepository,
                        RemoteAccessConsentRepository consentRepository,
                        AuditService auditService,
                        RealtimeService realtimeService) {
        this.mediaRepository = mediaRepository;
        this.deviceService = deviceService;
        this.permissionRepository = permissionRepository;
        this.consentRepository = consentRepository;
        this.auditService = auditService;
        this.realtimeService = realtimeService;
    }

    @Transactional(readOnly = true)
    public MediaDtos.MediaPage listForAdmin(AppPrincipal admin, Long deviceId, String mediaType, String search,
                                            String fromDate, String toDate, String sort, int page, int size) {
        Device device = deviceService.getOwnedDevice(admin, deviceId);
        requireConsent(device, admin.getId(), "view");
        Specification<MediaMetadata> spec = (root, q, cb) -> {
            List<jakarta.persistence.criteria.Predicate> preds = new ArrayList<>();
            preds.add(cb.equal(root.get("device").get("id"), device.getId()));
            preds.add(cb.equal(root.get("deletedOnDevice"), false));
            if (StringUtils.hasText(mediaType)) preds.add(cb.equal(root.get("mediaType"), mediaType.toUpperCase()));
            if (StringUtils.hasText(search)) {
                String like = "%" + search.toLowerCase() + "%";
                preds.add(cb.like(cb.lower(root.get("fileName")), like));
            }
            if (StringUtils.hasText(fromDate)) {
                Instant from = LocalDate.parse(fromDate).atStartOfDay().toInstant(ZoneOffset.UTC);
                preds.add(cb.greaterThanOrEqualTo(root.get("capturedAt"), from));
            }
            if (StringUtils.hasText(toDate)) {
                Instant to = LocalDate.parse(toDate).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
                preds.add(cb.lessThan(root.get("capturedAt"), to));
            }
            return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        Sort s = resolveSort(sort);
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), s);
        Page<MediaMetadata> p = mediaRepository.findAll(spec, pr);
        return new MediaDtos.MediaPage(p.map(this::toView).getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Transactional(readOnly = true)
    public MediaDtos.MediaView getForAdmin(AppPrincipal admin, Long deviceId, Long mediaId) {
        Device device = deviceService.getOwnedDevice(admin, deviceId);
        requireConsent(device, admin.getId(), "view");
        MediaMetadata m = mediaRepository.findByIdAndDeviceId(mediaId, device.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
        return toView(m);
    }

    @Transactional(readOnly = true)
    public MediaDtos.MediaContractResponse thumbnailContract(AppPrincipal admin, Long deviceId, Long mediaId) {
        Device device = deviceService.getOwnedDevice(admin, deviceId);
        requireConsent(device, admin.getId(), "stream");
        MediaMetadata m = mediaRepository.findByIdAndDeviceId(mediaId, device.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
        boolean available = m.isThumbnailAvailable() && !m.isDeletedOnDevice();
        String endpoint = "/api/admin/devices/" + deviceId + "/media/" + mediaId + "/thumbnail";
        String msg = available
                ? "Thumbnail available via device-provided reference. Actual retrieval is fulfilled by authorized Android device integration."
                : "No thumbnail available. Media metadata exists but device has not supplied a thumbnail reference.";
        return new MediaDtos.MediaContractResponse(m.getId(), device.getId(), m.getExternalId(), m.getMediaType(), available,
                available ? "DEVICE_THUMBNAIL" : "UNAVAILABLE", endpoint, msg);
    }

    @Transactional(readOnly = true)
    public MediaDtos.MediaContractResponse streamContract(AppPrincipal admin, Long deviceId, Long mediaId) {
        Device device = deviceService.getOwnedDevice(admin, deviceId);
        requireConsent(device, admin.getId(), "stream");
        DevicePermission perm = permissionRepository.findByDeviceId(device.getId()).orElse(null);
        if (perm != null && !perm.isCanStream()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Streaming not permitted for this device");
        }
        MediaMetadata m = mediaRepository.findByIdAndDeviceId(mediaId, device.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Media not found"));
        boolean available = m.isStreamAvailable() && !m.isDeletedOnDevice();
        String endpoint = "/api/admin/devices/" + deviceId + "/media/" + mediaId + "/stream";
        String msg = available
                ? "On-demand stream contract available. Actual media bytes are served by the authorized Android device/app integration when connected."
                : "Stream not available. No device-supplied stream reference for this media item.";
        return new MediaDtos.MediaContractResponse(m.getId(), device.getId(), m.getExternalId(), m.getMediaType(), available,
                available ? "DEVICE_STREAM" : "UNAVAILABLE", endpoint, msg);
    }

    @Transactional
    public void syncFromDevice(AppPrincipal devicePrincipal, MediaDtos.DeviceMediaSyncRequest req, HttpServletRequest request) {
        Device device = deviceService.getActiveDeviceForPrincipal(devicePrincipal);
        DevicePermission perm = permissionRepository.findByDeviceId(device.getId()).orElse(null);
        if (perm != null && !perm.isCanSyncMetadata()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Metadata sync not permitted");
        }
        int upserts = 0;
        if (req.items() != null) {
            for (MediaDtos.DeviceMediaSyncItem item : req.items()) {
                if (!StringUtils.hasText(item.externalId()) || !StringUtils.hasText(item.mediaType())) continue;
                String type = item.mediaType().toUpperCase();
                if (!type.equals("IMAGE") && !type.equals("VIDEO")) continue;
                MediaMetadata m = mediaRepository.findByDeviceIdAndExternalId(device.getId(), item.externalId()).orElseGet(MediaMetadata::new);
                boolean isNew = m.getId() == null;
                m.setDevice(device);
                m.setExternalId(item.externalId());
                m.setMediaType(type);
                m.setFileName(item.fileName());
                m.setMimeType(item.mimeType());
                m.setSizeBytes(item.sizeBytes());
                m.setWidth(item.width());
                m.setHeight(item.height());
                m.setDurationMs(item.durationMs());
                m.setCapturedAt(item.capturedAt());
                if (item.thumbnailAvailable() != null) m.setThumbnailAvailable(item.thumbnailAvailable());
                if (item.streamAvailable() != null) m.setStreamAvailable(item.streamAvailable());
                m.setThumbnailRef(item.thumbnailRef());
                m.setStreamRef(item.streamRef());
                m.setChecksum(item.checksum());
                if (item.deletedOnDevice() != null) m.setDeletedOnDevice(item.deletedOnDevice());
                m.setIndexedAt(Instant.now());
                mediaRepository.save(m);
                upserts++;
            }
        }
        device.setLastSeenAt(Instant.now());
        auditService.logDevice(device.getId(), "MEDIA_METADATA_SYNC", "MEDIA_METADATA", String.valueOf(device.getId()),
                "Synced " + upserts + " media metadata items", ip(request), true);
        realtimeService.galleryUpdated(device.getId(), "SYNC", java.util.Map.of("count", upserts));
    }

    private void requireConsent(Device device, Long adminId, String purpose) {
        if (device.isRevoked()) throw new ApiException(HttpStatus.FORBIDDEN, "Device is revoked");
        var consentOpt = consentRepository.findTopByDeviceIdAndAdminIdOrderByCreatedAtDesc(device.getId(), adminId);
        if (consentOpt.isEmpty() || !consentOpt.get().isGranted()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Remote access consent not granted by device owner");
        }
        var c = consentOpt.get();
        if (c.getExpiresAt() != null && c.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Remote access consent has expired");
        }
    }

    private Sort resolveSort(String sort) {
        if (!StringUtils.hasText(sort)) return Sort.by(Sort.Direction.DESC, "capturedAt").and(Sort.by(Sort.Direction.DESC, "indexedAt"));
        return switch (sort) {
            case "capturedAt,asc" -> Sort.by(Sort.Direction.ASC, "capturedAt");
            case "capturedAt,desc" -> Sort.by(Sort.Direction.DESC, "capturedAt");
            case "name,asc" -> Sort.by(Sort.Direction.ASC, "fileName");
            case "name,desc" -> Sort.by(Sort.Direction.DESC, "fileName");
            case "indexedAt,asc" -> Sort.by(Sort.Direction.ASC, "indexedAt");
            default -> Sort.by(Sort.Direction.DESC, "indexedAt");
        };
    }

    private MediaDtos.MediaView toView(MediaMetadata m) {
        return new MediaDtos.MediaView(
                m.getId(), m.getDevice().getId(), m.getExternalId(), m.getMediaType(), m.getFileName(), m.getMimeType(),
                m.getSizeBytes(), m.getWidth(), m.getHeight(), m.getDurationMs(), m.getCapturedAt(),
                m.isThumbnailAvailable(), m.isStreamAvailable(), m.isDeletedOnDevice(), m.getIndexedAt()
        );
    }

    private String ip(HttpServletRequest request) {
        if (request == null) return null;
        String fwd = request.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) return fwd.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
