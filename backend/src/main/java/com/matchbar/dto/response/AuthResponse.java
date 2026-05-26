package com.matchbar.dto.response;

import com.matchbar.entity.User;

public record AuthResponse(
        String token,
        Long userId,
        String email,
        String name,
        User.Role role
) {}
