package com.gallery.platform.media.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public class MediaDtos {
    public record MediaView(
            Long id,
            Long deviceId,
            String externalId,
            String mediaType,
            String fileName,
            String mimeType,
            Long sizeBytes,
            Integer width,
            Integer height,
            Long durationMs,
            Instant capturedAt,
            boolean thumbnailAvailable,
            boolean streamAvailable,
            boolean deletedOnDevice,
            Instant indexedAt
    ) {}

    public record MediaPage(
            List<MediaView> items,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record DeviceMediaSyncItem(
            @NotBlank String externalId,
            @NotBlank String mediaType, // IMAGE, VIDEO
            String fileName,
            String mimeType,
            Long sizeBytes,
            Integer width,
            Integer height,
            Long durationMs,
            Instant capturedAt,
            Boolean thumbnailAvailable,
            Boolean streamAvailable,
            String thumbnailRef,
            String streamRef,
            String checksum,
            Boolean deletedOnDevice
    ) {}

    public record DeviceMediaSyncRequest(@NotNull List<DeviceMediaSyncItem> items) {}

    public record MediaContractResponse(
            Long mediaId,
            Long deviceId,
            String externalId,
            String mediaType,
            boolean available,
            String contract, // e.g. DEVICE_STREAM, DEVICE_THUMBNAIL, UNAVAILABLE
            String endpoint,
            String message
    ) {}
}
