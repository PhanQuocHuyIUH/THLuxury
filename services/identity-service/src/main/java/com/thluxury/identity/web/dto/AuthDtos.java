package com.thluxury.identity.web.dto;

import com.thluxury.identity.domain.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 200) String fullName,
            @Size(max = 20) String phone
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(@NotBlank String refreshToken) {}

    public record UserView(
            UUID id,
            String email,
            String fullName,
            String phone,
            UserRole role,
            UUID branchId,
            boolean enabled,
            OffsetDateTime createdAt
    ) {}

    public record TokenPair(
            String accessToken,
            String refreshToken,
            long accessTtlSeconds,
            UserView user
    ) {}

    public record UpdateMeRequest(
            @Size(max = 200) String fullName,
            @Size(max = 20) String phone
    ) {}

    public record ForgotPasswordRequest(@Email @NotBlank String email) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 100) String password
    ) {}

    public record GoogleLoginRequest(@NotBlank String idToken) {}
}
