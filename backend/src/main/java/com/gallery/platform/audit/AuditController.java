package com.gallery.platform.audit;

import com.gallery.platform.audit.dto.AuditDtos;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public Page<AuditDtos.AuditLogView> list(@RequestParam(required = false) Long deviceId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        return auditService.list(deviceId, page, size);
    }
}
