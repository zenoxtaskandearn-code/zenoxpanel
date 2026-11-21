package com.gallery.platform.device;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceSessionRepository extends JpaRepository<DeviceSession, Long> {
    Optional<DeviceSession> findBySessionTokenHashAndActiveTrue(String sessionTokenHash);
}
