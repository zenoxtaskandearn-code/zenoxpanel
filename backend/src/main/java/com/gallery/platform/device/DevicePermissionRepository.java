package com.gallery.platform.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DevicePermissionRepository extends JpaRepository<DevicePermission, Long> {
    Optional<DevicePermission> findByDeviceId(Long deviceId);
}
