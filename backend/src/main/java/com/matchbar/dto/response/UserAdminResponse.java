package com.matchbar.dto.response;

import com.matchbar.entity.User;
import java.time.Instant;

public record UserAdminResponse(
        String id,
        String name,
        String email,
        User.Role role,
        Instant createdAt
) {}
