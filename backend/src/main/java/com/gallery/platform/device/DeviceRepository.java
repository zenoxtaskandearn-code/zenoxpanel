package com.gallery.platform.device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long>, JpaSpecificationExecutor<Device> {
    Optional<Device> findByDeviceUuid(String deviceUuid);
    Optional<Device> findByIdAndOwnerAdminId(Long id, Long ownerAdminId);
    List<Device> findByOwnerAdminId(Long ownerAdminId);
    long countByOwnerAdminId(Long ownerAdminId);
    long countByOwnerAdminIdAndStatus(Long ownerAdminId, String status);
}
