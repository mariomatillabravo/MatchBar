package com.matchbar.security;

import com.matchbar.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    @Test
    void sinSecretoNoArranca() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("", 3_600_000));
    }

    @Test
    void secretoDemasiadoCortoNoArranca() {
        String corto = "x".repeat(JwtTokenProvider.MIN_SECRET_BYTES - 1);
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(corto, 3_600_000));
    }

    @Test
    void secretoValidoFirmaYLeeTokens() {
        JwtTokenProvider provider = new JwtTokenProvider("x".repeat(JwtTokenProvider.MIN_SECRET_BYTES), 3_600_000);
        User user = User.builder().id("u1").email("a@test.com").name("A").role(User.Role.USER).build();

        assertEquals("u1", provider.getUserId(provider.generateToken(user)));
    }
}
