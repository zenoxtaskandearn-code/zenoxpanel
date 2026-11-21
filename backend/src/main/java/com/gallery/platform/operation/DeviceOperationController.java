package com.gallery.platform.operation;

import com.gallery.platform.operation.dto.OperationDtos;
import com.gallery.platform.security.AppPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/device/operations")
public class DeviceOperationController {
    private final OperationService operationService;

    public DeviceOperationController(OperationService operationService) {
        this.operationService = operationService;
    }

    @GetMapping("/pending")
    public List<OperationDtos.OperationView> pending(@AuthenticationPrincipal AppPrincipal principal) {
        return operationService.pendingForDevice(principal);
    }

    @PostMapping("/{id}/ack")
    public ResponseEntity<OperationDtos.OperationView> ack(@AuthenticationPrincipal AppPrincipal principal,
                                                           @PathVariable Long id,
                                                           @RequestBody(required = false) OperationDtos.DeviceAckRequest req,
                                                           HttpServletRequest request) {
        return ResponseEntity.ok(operationService.ack(principal, id, req, request));
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<OperationDtos.OperationView> status(@AuthenticationPrincipal AppPrincipal principal,
                                                              @PathVariable Long id,
                                                              @Valid @RequestBody OperationDtos.DeviceStatusUpdateRequest req,
                                                              HttpServletRequest request) {
        return ResponseEntity.ok(operationService.updateStatus(principal, id, req, request));
    }
}
