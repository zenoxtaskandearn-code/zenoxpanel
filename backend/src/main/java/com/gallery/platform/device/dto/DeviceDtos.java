package com.gallery.platform.device.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class DeviceDtos {
    public record DeviceView(
            Long id,
            String deviceUuid,
            String name,
            String deviceType,
            String status,
            boolean revoked,
            Instant lastSeenAt,
            String lastKnownIp,
            String appVersion,
            Instant createdAt,
            PermissionView permissions,
            ConsentView latestConsent
    ) {}

    public record PermissionView(boolean canView, boolean canStream, boolean canEdit, boolean canDelete, boolean canSyncMetadata) {}
    public record ConsentView(Long id, boolean granted, String scope, Instant grantedAt, Instant revokedAt, Instant expiresAt) {}

    public record AdminCreateDeviceRequest(
            @NotBlank @Size(max = 100) String deviceUuid,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 50) String deviceType
    ) {}

    public record AdminUpdateDeviceRequest(
            @Size(max = 150) String name,
            @Size(max = 50) String deviceType
    ) {}

    public record PermissionUpdateRequest(
            @NotNull Boolean canView,
            @NotNull Boolean canStream,
            @NotNull Boolean canEdit,
            @NotNull Boolean canDelete,
            @NotNull Boolean canSyncMetadata
    ) {}

    public record ConsentRequest(
            @NotNull Boolean granted,
            String scope,
            Instant expiresAt,
            String notes
    ) {}

    public record DeviceRegisterRequest(
            @NotBlank @Size(max = 100) String deviceUuid,
            @NotBlank @Size(max = 150) String name,
            @Size(max = 50) String deviceType,
            String appVersion,
            String deviceSecret
    ) {}

    public record DeviceAuthRequest(
            @NotBlank String deviceUuid,
            @NotBlank String deviceSecret
    ) {}

    public record DeviceAuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            DeviceView device
    ) {}

    public record HeartbeatRequest(String appVersion) {}
    public record DeviceStatusRequest(@NotBlank String status) {}
}
