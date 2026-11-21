package com.gallery.platform.device;

import com.gallery.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "device_sessions")
public class DeviceSession extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "session_token_hash", nullable = false)
    private String sessionTokenHash;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt = Instant.now();

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @Column(name = "last_heartbeat_at")
    private Instant lastHeartbeatAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "disconnect_reason")
    private String disconnectReason;
}
