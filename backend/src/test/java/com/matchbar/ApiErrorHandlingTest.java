package com.matchbar;

import com.matchbar.config.SecurityConfig;
import com.matchbar.controller.AuthController;
import com.matchbar.controller.BarController;
import com.matchbar.controller.MatchController;
import com.matchbar.entity.User;
import com.matchbar.repository.UserRepository;
import com.matchbar.security.JsonSecurityErrorHandler;
import com.matchbar.security.JwtTokenProvider;
import com.matchbar.service.AuthService;
import com.matchbar.service.BarService;
import com.matchbar.service.ImageService;
import com.matchbar.service.LicenseDocService;
import com.matchbar.service.MatchService;
import com.matchbar.service.ReviewService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprueba que la API responde con el código HTTP correcto y el cuerpo JSON
 * común ({status, message}) en lugar de un 500 genérico. Usa la configuración
 * de seguridad real con los servicios simulados (no necesita MongoDB).
 */
@WebMvcTest(controllers = {MatchController.class, BarController.class, AuthController.class},
        properties = "matchbar.jwt.secret=" + TestSecrets.JWT_SECRET)
@Import({SecurityConfig.class, JsonSecurityErrorHandler.class, JwtTokenProvider.class})
class ApiErrorHandlingTest {

    static final String SECRET = TestSecrets.JWT_SECRET;

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider tokenProvider;

    @MockitoBean UserRepository userRepository;
    @MockitoBean MatchService matchService;
    @MockitoBean BarService barService;
    @MockitoBean ReviewService reviewService;
    @MockitoBean LicenseDocService licenseDocService;
    @MockitoBean ImageService imageService;
    @MockitoBean AuthService authService;

    private String userToken;

    @BeforeEach
    void setUp() {
        User user = User.builder().id("u1").email("mario@test.com").name("Mario").role(User.Role.USER).build();
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        userToken = "Bearer " + tokenProvider.generateToken(user);
    }

    @Test
    void sinTokenDevuelve401EnJson() throws Exception {
        mvc.perform(get("/api/matches"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Sesión no iniciada o caducada"));
    }

    @Test
    void tokenCaducadoDevuelve401() throws Exception {
        String expired = Jwts.builder()
                .subject("u1")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        mvc.perform(get("/api/matches").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolInsuficienteEnPreAuthorizeDevuelve403YNo500() throws Exception {
        // Un USER intentando crear ficha de bar (solo BAR).
        mvc.perform(post("/api/bars/me").header("Authorization", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mi bar\",\"address\":\"Calle Mayor 1, Madrid\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("No tienes permisos para esta acción"));
    }

    @Test
    void rutaAdminSinRolAdminDevuelve403EnJson() throws Exception {
        mvc.perform(get("/api/admin/bars/pending").header("Authorization", userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void parametroObligatorioAusenteDevuelve400() throws Exception {
        mvc.perform(get("/api/bars/nearby").param("lng", "-3.7").header("Authorization", userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("'lat'")));
    }

    @Test
    void parametroConTipoInvalidoDevuelve400() throws Exception {
        mvc.perform(get("/api/bars/nearby").param("lat", "abc").param("lng", "-3.7")
                        .header("Authorization", userToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("'lat'")));
    }

    @Test
    void jsonMalFormadoDevuelve400() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{roto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la petición no es válido"));
    }

    @Test
    void claveDuplicadaDevuelve409() throws Exception {
        when(authService.register(any())).thenThrow(new DuplicateKeyException("E11000 duplicate key"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nuevo@test.com\",\"password\":\"password123\",\"name\":\"Nuevo\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void errorInesperadoDevuelve500SinDetallesInternos() throws Exception {
        when(matchService.search(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("detalle interno secreto"));

        mvc.perform(get("/api/matches").header("Authorization", userToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(content().string(not(containsString("secreto"))));
    }

    @Test
    void laEntradaDeSwaggerUiNoExigeSesion() throws Exception {
        // En este test no se carga springdoc: un 404 (y no un 401) demuestra
        // que la ruta documentada /swagger-ui.html es pública.
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isNotFound());
    }

    @Test
    void rutaInexistenteDevuelve404() throws Exception {
        mvc.perform(get("/api/esto-no-existe").header("Authorization", userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Recurso no encontrado"));
    }
}
