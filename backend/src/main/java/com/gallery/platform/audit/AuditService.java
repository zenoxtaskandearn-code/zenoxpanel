package com.gallery.platform.audit;

import com.gallery.platform.device.Device;
import com.gallery.platform.device.DeviceRepository;
import com.gallery.platform.audit.dto.AuditDtos;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditLogRepository auditLogRepository;
    private final DeviceRepository deviceRepository;

    public AuditService(AuditLogRepository auditLogRepository, DeviceRepository deviceRepository) {
        this.auditLogRepository = auditLogRepository;
        this.deviceRepository = deviceRepository;
    }

    @Transactional
    public void logAdmin(Long adminId, String action, String resourceType, String resourceId, String details, String ip, boolean success, Long deviceId) {
        AuditLog log = base("ADMIN", adminId == null ? null : String.valueOf(adminId), action, resourceType, resourceId, details, ip, success);
        attachDevice(log, deviceId);
        auditLogRepository.save(log);
    }

    @Transactional
    public void logDevice(Long deviceId, String action, String resourceType, String resourceId, String details, String ip, boolean success) {
        AuditLog log = base("DEVICE", deviceId == null ? null : String.valueOf(deviceId), action, resourceType, resourceId, details, ip, success);
        attachDevice(log, deviceId);
        auditLogRepository.save(log);
    }

    @Transactional
    public void logSystem(String action, String resourceType, String resourceId, String details, boolean success, Long deviceId) {
        AuditLog log = base("SYSTEM", "system", action, resourceType, resourceId, details, null, success);
        attachDevice(log, deviceId);
        auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public Page<AuditDtos.AuditLogView> list(Long deviceId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuditLog> p = (deviceId != null) ? auditLogRepository.findByDeviceId(deviceId, pr) : auditLogRepository.findAll(pr);
        return p.map(this::toView);
    }

    private AuditLog base(String actorType, String actorId, String action, String resourceType, String resourceId, String details, String ip, boolean success) {
        AuditLog log = new AuditLog();
        log.setActorType(actorType);
        log.setActorId(actorId);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetails(details);
        log.setIpAddress(ip);
        log.setSuccess(success);
        return log;
    }

    private void attachDevice(AuditLog log, Long deviceId) {
        if (deviceId != null) {
            Device d = deviceRepository.findById(deviceId).orElse(null);
            log.setDevice(d);
        }
    }

    private AuditDtos.AuditLogView toView(AuditLog l) {
        return new AuditDtos.AuditLogView(
                l.getId(), l.getActorType(), l.getActorId(),
                l.getDevice() != null ? l.getDevice().getId() : null,
                l.getAction(), l.getResourceType(), l.getResourceId(),
                l.getDetails(), l.getIpAddress(), l.isSuccess(),
                l.getCreatedAt()
        );
    }
}
