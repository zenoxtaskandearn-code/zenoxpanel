package com.gallery.platform.media;

import com.gallery.platform.common.BaseEntity;
import com.gallery.platform.device.Device;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "media_metadata")
public class MediaMetadata extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "media_type", nullable = false)
    private String mediaType; // IMAGE, VIDEO

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    private Integer width;
    private Integer height;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "captured_at")
    private Instant capturedAt;

    @Column(name = "thumbnail_available", nullable = false)
    private boolean thumbnailAvailable = false;

    @Column(name = "stream_available", nullable = false)
    private boolean streamAvailable = false;

    @Column(name = "thumbnail_ref")
    private String thumbnailRef;

    @Column(name = "stream_ref")
    private String streamRef;

    private String checksum;

    @Column(name = "deleted_on_device", nullable = false)
    private boolean deletedOnDevice = false;

    @Column(name = "indexed_at", nullable = false)
    private Instant indexedAt = Instant.now();
}
