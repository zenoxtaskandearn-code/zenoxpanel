package com.gallery.platform.media;

import com.gallery.platform.media.dto.MediaDtos;
import com.gallery.platform.security.AppPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/devices/{deviceId}/media")
public class AdminMediaController {
    private final MediaService mediaService;

    public AdminMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    public MediaDtos.MediaPage list(@AuthenticationPrincipal AppPrincipal principal,
                                    @PathVariable Long deviceId,
                                    @RequestParam(required = false) String mediaType,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) String from,
                                    @RequestParam(required = false) String to,
                                    @RequestParam(required = false) String sort,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "24") int size) {
        return mediaService.listForAdmin(principal, deviceId, mediaType, search, from, to, sort, page, size);
    }

    @GetMapping("/{mediaId}")
    public MediaDtos.MediaView get(@AuthenticationPrincipal AppPrincipal principal,
                                   @PathVariable Long deviceId,
                                   @PathVariable Long mediaId) {
        return mediaService.getForAdmin(principal, deviceId, mediaId);
    }

    @GetMapping("/{mediaId}/thumbnail")
    public MediaDtos.MediaContractResponse thumbnail(@AuthenticationPrincipal AppPrincipal principal,
                                                     @PathVariable Long deviceId,
                                                     @PathVariable Long mediaId) {
        return mediaService.thumbnailContract(principal, deviceId, mediaId);
    }

    @GetMapping("/{mediaId}/stream")
    public MediaDtos.MediaContractResponse stream(@AuthenticationPrincipal AppPrincipal principal,
                                                  @PathVariable Long deviceId,
                                                  @PathVariable Long mediaId) {
        return mediaService.streamContract(principal, deviceId, mediaId);
    }
}
