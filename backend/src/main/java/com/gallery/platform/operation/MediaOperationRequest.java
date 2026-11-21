package com.gallery.platform.operation;

import com.gallery.platform.admin.Admin;
import com.gallery.platform.common.BaseEntity;
import com.gallery.platform.device.Device;
import com.gallery.platform.media.MediaMetadata;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "media_operation_requests")
public class MediaOperationRequest extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    private MediaMetadata media;

    @Column(name = "operation_type", nullable = false)
    private String operationType; // PREVIEW, EDIT, DELETE

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private String status = "PENDING"; // PENDING, ACKNOWLEDGED, IN_PROGRESS, COMPLETED, FAILED, EXPIRED, CANCELLED

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "device_ack_message")
    private String deviceAckMessage;
}
