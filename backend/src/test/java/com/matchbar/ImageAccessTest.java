package com.matchbar;

import com.matchbar.config.SecurityConfig;
import com.matchbar.controller.AdminController;
import com.matchbar.controller.BarController;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.BarRepository;
import com.matchbar.repository.UserRepository;
import com.matchbar.security.JsonSecurityErrorHandler;
import com.matchbar.security.JwtTokenProvider;
import com.matchbar.service.BarService;
import com.matchbar.service.ImageService;
import com.matchbar.service.IncidentService;
import com.matchbar.service.LicenseDocService;
import com.matchbar.service.MatchService;
import com.matchbar.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Quién puede ver cada imagen guardada en GridFS. */
@WebMvcTest(controllers = {BarController.class, AdminController.class},
        properties = "matchbar.jwt.secret=" + TestSecrets.JWT_SECRET)
@Import({SecurityConfig.class, JsonSecurityErrorHandler.class, JwtTokenProvider.class})
class ImageAccessTest {

    @Autowired MockMvc mvc;
    @Autowired JwtTokenProvider tokenProvider;

    @MockBean UserRepository userRepository;
    @MockBean BarRepository barRepository;
    @MockBean BarService barService;
    @MockBean ReviewService reviewService;
    @MockBean LicenseDocService licenseDocService;
    @MockBean ImageService imageService;
    @MockBean MatchService matchService;
    @MockBean IncidentService incidentService;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        userToken = tokenFor(User.builder().id("u1").email("mario@test.com").name("Mario").role(User.Role.USER).build());
        adminToken = tokenFor(User.builder().id("a1").email("admin@matchbar.com").name("Admin").role(User.Role.ADMIN).build());
    }

    @Test
    void lasFotosDeUnBarSonPublicas() throws Exception {
        when(barService.isPublicBarImage("foto-bar")).thenReturn(true);
        GridFsResource jpeg = image("image/jpeg");
        when(imageService.load("foto-bar")).thenReturn(jpeg);

        mvc.perform(get("/api/bars/images/foto-bar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));
    }

    @Test
    void unaImagenQueNoEsDeUnBarNoSeSirvePorElEndpointPublico() throws Exception {
        when(barService.isPublicBarImage("foto-incidencia")).thenReturn(false);

        mvc.perform(get("/api/bars/images/foto-incidencia"))
                .andExpect(status().isNotFound());
        verify(imageService, never()).load("foto-incidencia");
    }

    @Test
    void lasFotosDeIncidenciasExigenSerAdmin() throws Exception {
        mvc.perform(get("/api/admin/incidents/i1/photos/f1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/incidents/i1/photos/f1").header("Authorization", userToken))
                .andExpect(status().isForbidden());
        verify(incidentService, never()).loadPhoto("i1", "f1");
    }

    @Test
    void elAdminVeLasFotosDeUnaIncidencia() throws Exception {
        GridFsResource png = image("image/png");
        when(incidentService.loadPhoto("i1", "f1")).thenReturn(png);

        mvc.perform(get("/api/admin/incidents/i1/photos/f1").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void unaFotoDeOtraIncidenciaDevuelve404() throws Exception {
        when(incidentService.loadPhoto("i1", "f-ajena"))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Imagen no encontrada"));

        mvc.perform(get("/api/admin/incidents/i1/photos/f-ajena").header("Authorization", adminToken))
                .andExpect(status().isNotFound());
    }

    private String tokenFor(User user) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        return "Bearer " + tokenProvider.generateToken(user);
    }

    private static GridFsResource image(String contentType) throws IOException {
        GridFsResource resource = mock(GridFsResource.class);
        when(resource.getContentType()).thenReturn(contentType);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));
        return resource;
    }
}
