package com.gallery.platform.device;

import com.gallery.platform.device.dto.DeviceDtos;
import com.gallery.platform.security.AppPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/devices")
public class AdminDeviceController {
    private final DeviceService deviceService;

    public AdminDeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceDtos.DeviceView> list(@AuthenticationPrincipal AppPrincipal principal,
                                            @RequestParam(required = false) String search,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) Boolean revoked) {
        return deviceService.listForAdmin(principal, search, status, revoked);
    }

    @PostMapping
    public ResponseEntity<DeviceDtos.DeviceView> create(@AuthenticationPrincipal AppPrincipal principal,
                                                        @Valid @RequestBody DeviceDtos.AdminCreateDeviceRequest req,
                                                        HttpServletRequest request) {
        return ResponseEntity.ok(deviceService.adminCreate(principal, req, request));
    }

    @GetMapping("/{id}")
    public DeviceDtos.DeviceView get(@AuthenticationPrincipal AppPrincipal principal, @PathVariable Long id) {
        return deviceService.getForAdmin(principal, id);
    }

    @PutMapping("/{id}")
    public DeviceDtos.DeviceView update(@AuthenticationPrincipal AppPrincipal principal,
                                        @PathVariable Long id,
                                        @RequestBody DeviceDtos.AdminUpdateDeviceRequest req,
                                        HttpServletRequest request) {
        return deviceService.adminUpdate(principal, id, req, request);
    }

    @PostMapping("/{id}/revoke")
    public DeviceDtos.DeviceView revoke(@AuthenticationPrincipal AppPrincipal principal,
                                        @PathVariable Long id,
                                        @RequestParam(defaultValue = "true") boolean revoked,
                                        HttpServletRequest request) {
        return deviceService.revoke(principal, id, revoked, request);
    }

    @PutMapping("/{id}/permissions")
    public DeviceDtos.DeviceView permissions(@AuthenticationPrincipal AppPrincipal principal,
                                             @PathVariable Long id,
                                             @Valid @RequestBody DeviceDtos.PermissionUpdateRequest req,
                                             HttpServletRequest request) {
        return deviceService.updatePermissions(principal, id, req, request);
    }

    @PostMapping("/{id}/consent")
    public DeviceDtos.DeviceView consent(@AuthenticationPrincipal AppPrincipal principal,
                                         @PathVariable Long id,
                                         @Valid @RequestBody DeviceDtos.ConsentRequest req,
                                         HttpServletRequest request) {
        return deviceService.setConsent(principal, id, req, request);
    }
}
