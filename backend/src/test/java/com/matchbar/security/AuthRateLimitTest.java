package com.matchbar.security;

import com.matchbar.TestSecrets;
import com.matchbar.config.SecurityConfig;
import com.matchbar.controller.AuthController;
import com.matchbar.repository.UserRepository;
import com.matchbar.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Límite de peticiones por IP en /api/auth/login (configurado a 3/min en este test). */
@WebMvcTest(controllers = AuthController.class, properties = {
        "matchbar.jwt.secret=" + TestSecrets.JWT_SECRET,
        "matchbar.rate-limit.login-per-minute=3"})
@Import({SecurityConfig.class, JsonSecurityErrorHandler.class, JwtTokenProvider.class})
class AuthRateLimitTest {

    @Autowired MockMvc mvc;
    @MockBean AuthService authService;
    @MockBean UserRepository userRepository;

    @Test
    void superarElLimiteDeLoginDevuelve429SoloParaEsaIp() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(login("198.51.100.1")).andExpect(status().isOk());
        }

        mvc.perform(login("198.51.100.1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.status").value(429));

        mvc.perform(login("198.51.100.2")).andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder login(String clientIp) {
        return post("/api/auth/login")
                .with(request -> { request.setRemoteAddr(clientIp); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"mario@test.com\",\"password\":\"password123\"}");
    }
}
