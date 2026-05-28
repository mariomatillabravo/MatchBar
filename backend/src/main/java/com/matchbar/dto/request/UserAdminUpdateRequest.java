package com.matchbar.dto.request;

import com.matchbar.entity.User;
import jakarta.validation.constraints.*;

public record UserAdminUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email String email,
        @NotNull User.Role role,
        @Size(min = 6, max = 100) String password
) {}
