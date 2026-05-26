package com.matchbar.dto.request;

import jakarta.validation.constraints.*;

public record ReviewRequest(
        @NotNull @Min(1) @Max(5) Integer ratingAtmosphere,
        @NotNull @Min(1) @Max(5) Integer ratingFood,
        @NotNull @Min(1) @Max(5) Integer ratingPrice,
        @Size(max = 500) String comment
) {}
