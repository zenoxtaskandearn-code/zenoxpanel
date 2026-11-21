package com.gallery.platform.operation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MediaOperationRequestRepository extends JpaRepository<MediaOperationRequest, Long> {
    Optional<MediaOperationRequest> findByIdAndDeviceId(Long id, Long deviceId);
    Page<MediaOperationRequest> findByDeviceId(Long deviceId, Pageable pageable);
    Page<MediaOperationRequest> findByAdminId(Long adminId, Pageable pageable);
    List<MediaOperationRequest> findByStatusAndExpiresAtBefore(String status, Instant now);
    List<MediaOperationRequest> findByDeviceIdAndStatusIn(Long deviceId, List<String> statuses);
}
