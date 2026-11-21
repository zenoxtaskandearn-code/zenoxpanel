package com.gallery.platform.audit.dto;

import java.time.Instant;

public class AuditDtos {
    public record AuditLogView(
            Long id,
            String actorType,
            String actorId,
            Long deviceId,
            String action,
            String resourceType,
            String resourceId,
            String details,
            String ipAddress,
            boolean success,
            Instant createdAt
    ) {}
}
