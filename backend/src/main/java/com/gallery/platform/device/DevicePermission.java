package com.gallery.platform.device;

import com.gallery.platform.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "device_permissions")
public class DevicePermission extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false, unique = true)
    private Device device;

    @Column(name = "can_view", nullable = false)
    private boolean canView = true;
    @Column(name = "can_stream", nullable = false)
    private boolean canStream = false;
    @Column(name = "can_edit", nullable = false)
    private boolean canEdit = false;
    @Column(name = "can_delete", nullable = false)
    private boolean canDelete = false;
    @Column(name = "can_sync_metadata", nullable = false)
    private boolean canSyncMetadata = true;
}
