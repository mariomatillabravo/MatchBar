package com.matchbar.config;

import com.matchbar.TestSecrets;
import com.matchbar.controller.AuthController;
import com.matchbar.repository.UserRepository;
import com.matchbar.security.JsonSecurityErrorHandler;
import com.matchbar.security.JwtTokenProvider;
import com.matchbar.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class, properties = {
        "matchbar.jwt.secret=" + TestSecrets.JWT_SECRET,
        "matchbar.cors.allowed-origins=https://panel.matchbar.es"})
@Import({SecurityConfig.class, JsonSecurityErrorHandler.class, JwtTokenProvider.class})
class CorsConfigTest {

    @Autowired MockMvc mvc;
    @MockBean AuthService authService;
    @MockBean UserRepository userRepository;

    @Test
    void unOrigenNoAutorizadoSeRechaza() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void unOrigenAutorizadoRecibeLasCabecerasCors() throws Exception {
        mvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://panel.matchbar.es")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://panel.matchbar.es"));
    }

    @Test
    void lasPeticionesDelPropioDominioNoSeVenAfectadas() throws Exception {
        // El panel admin se sirve desde la API: su Origin coincide con el host.
        mvc.perform(post("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@matchbar.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }
}
