package com.gallery.platform.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RemoteAccessConsentRepository extends JpaRepository<RemoteAccessConsent, Long> {
    Optional<RemoteAccessConsent> findTopByDeviceIdAndAdminIdOrderByCreatedAtDesc(Long deviceId, Long adminId);
}
