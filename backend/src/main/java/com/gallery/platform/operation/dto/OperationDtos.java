package com.gallery.platform.operation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public class OperationDtos {
    public record CreateOperationRequest(
            @NotNull Long deviceId,
            Long mediaId,
            @NotBlank String operationType, // PREVIEW, EDIT, DELETE
            String payload,
            Integer expiresInMinutes
    ) {}

    public record OperationView(
            Long id,
            Long deviceId,
            Long adminId,
            Long mediaId,
            String operationType,
            String payload,
            String status,
            String failureReason,
            Instant requestedAt,
            Instant expiresAt,
            Instant acknowledgedAt,
            Instant completedAt,
            String deviceAckMessage
    ) {}

    public record DeviceAckRequest(String message, String status) {}
    public record DeviceStatusUpdateRequest(@NotBlank String status, String message, String failureReason) {}
}
