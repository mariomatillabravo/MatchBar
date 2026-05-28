package com.matchbar.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record BarAdminUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 2000) String description,
        @NotBlank String address,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @Size(max = 20) String ownerPhone,
        String photoUrl
) {}
