package com.gallery.platform.media;

import com.gallery.platform.media.dto.MediaDtos;
import com.gallery.platform.security.AppPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/device/media")
public class DeviceMediaController {
    private final MediaService mediaService;

    public DeviceMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> sync(@AuthenticationPrincipal AppPrincipal principal,
                                                    @Valid @RequestBody MediaDtos.DeviceMediaSyncRequest req,
                                                    HttpServletRequest request) {
        mediaService.syncFromDevice(principal, req, request);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
