package com.gallery.platform.media;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MediaMetadataRepository extends JpaRepository<MediaMetadata, Long>, JpaSpecificationExecutor<MediaMetadata> {
    Optional<MediaMetadata> findByDeviceIdAndExternalId(Long deviceId, String externalId);
    Optional<MediaMetadata> findByIdAndDeviceId(Long id, Long deviceId);
    Page<MediaMetadata> findByDeviceId(Long deviceId, Pageable pageable);

    @Query("select count(m) from MediaMetadata m where m.device.ownerAdmin.id = :adminId and m.mediaType = :type and m.deletedOnDevice = false")
    long countByAdminAndType(Long adminId, String type);
}
