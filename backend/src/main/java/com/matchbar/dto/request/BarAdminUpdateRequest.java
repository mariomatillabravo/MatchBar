package com.matchbar.dto.request;

import jakarta.validation.constraints.*;

public record BarAdminUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @NotBlank String address,
        @Size(max = 20) String ownerPhone
) {}
