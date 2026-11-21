package com.gallery.platform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 2, max = 150) String name,
            @NotBlank @Size(min = 8, max = 100) String password,
            String inviteCode
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record LogoutRequest(String refreshToken) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(min = 2, max = 150) String name
    ) {}

    public record AuthResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInSeconds,
            AdminProfile admin
    ) {}

    public record AdminProfile(Long id, String email, String name, String role) {}
}
