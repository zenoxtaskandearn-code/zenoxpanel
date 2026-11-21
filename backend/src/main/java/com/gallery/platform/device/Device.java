package com.gallery.platform.device;

import com.gallery.platform.admin.Admin;
import com.gallery.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "devices")
public class Device extends BaseEntity {
    @Column(name = "device_uuid", nullable = false, unique = true)
    private String deviceUuid;

    @Column(nullable = false)
    private String name;

    @Column(name = "device_type")
    private String deviceType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_admin_id", nullable = false)
    private Admin ownerAdmin;

    @Column(nullable = false)
    private String status = "OFFLINE";

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_known_ip")
    private String lastKnownIp;

    @Column(name = "app_version")
    private String appVersion;

    @Column(name = "device_secret_hash")
    private String deviceSecretHash;
}
