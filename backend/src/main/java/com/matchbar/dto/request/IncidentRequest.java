package com.matchbar.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IncidentRequest(
        @NotBlank @Size(max = 200) String subject,
        @NotBlank String message
) {}
