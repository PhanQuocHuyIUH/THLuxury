package com.thluxury.identity.web.dto;

import com.thluxury.identity.domain.UserRole;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public final class AdminDtos {
    private AdminDtos() {}

    public record CreateBranchManagerRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank @Size(max = 200) String fullName,
            @Size(max = 20) String phone,
            @NotNull UUID branchId
    ) {}

    public record UpdateUserRequest(
            @Size(max = 200) String fullName,
            @Size(max = 20) String phone,
            UserRole role,
            UUID branchId,
            Boolean enabled
    ) {}

    public record BranchView(
            UUID id,
            String code,
            String name,
            String address,
            String city,
            String district,
            String ward,
            String phone,
            BigDecimal lat,
            BigDecimal lng,
            boolean enabled
    ) {}

    public record CreateBranchRequest(
            @NotBlank @Size(max = 16) String code,
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) String address,
            @Size(max = 100) String city,
            @Size(max = 100) String district,
            @Size(max = 100) String ward,
            @Size(max = 20) String phone,
            @DecimalMin(value = "-90", inclusive = true) @DecimalMax(value = "90", inclusive = true) BigDecimal lat,
            @DecimalMin(value = "-180", inclusive = true) @DecimalMax(value = "180", inclusive = true) BigDecimal lng
    ) {}

    public record UpdateBranchRequest(
            @Size(max = 200) String name,
            @Size(max = 500) String address,
            @Size(max = 100) String city,
            @Size(max = 100) String district,
            @Size(max = 100) String ward,
            @Size(max = 20) String phone,
            BigDecimal lat,
            BigDecimal lng,
            Boolean enabled
    ) {}
}
