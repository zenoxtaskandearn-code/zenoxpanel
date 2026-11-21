package com.gallery.platform.device;

import com.gallery.platform.device.dto.DeviceDtos;
import com.gallery.platform.security.AppPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/device")
public class DeviceApiController {
    private final DeviceService deviceService;

    public DeviceApiController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping({"/register"})
    public ResponseEntity<DeviceDtos.DeviceAuthResponse> register(@Valid @RequestBody DeviceDtos.DeviceRegisterRequest req, HttpServletRequest request) {
        return ResponseEntity.ok(deviceService.deviceRegister(req, request));
    }

    @PostMapping({"/auth", "/login"})
    public ResponseEntity<DeviceDtos.DeviceAuthResponse> auth(@Valid @RequestBody DeviceDtos.DeviceAuthRequest req, HttpServletRequest request) {
        return ResponseEntity.ok(deviceService.deviceAuth(req, request));
    }

    @PostMapping("/heartbeat")
    public ResponseEntity<Map<String, String>> heartbeat(@AuthenticationPrincipal AppPrincipal principal,
                                                         @RequestBody(required = false) DeviceDtos.HeartbeatRequest req,
                                                         HttpServletRequest request) {
        deviceService.heartbeat(principal, req, request);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/status")
    public ResponseEntity<Map<String, String>> status(@AuthenticationPrincipal AppPrincipal principal,
                                                      @Valid @RequestBody DeviceDtos.DeviceStatusRequest req,
                                                      HttpServletRequest request) {
        deviceService.updateStatus(principal, req, request);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
