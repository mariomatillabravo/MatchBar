package com.matchbar.service;

import com.matchbar.entity.Incident;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.IncidentRepository;
import com.matchbar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncidentServiceTest {

    private final IncidentRepository incidentRepository = mock(IncidentRepository.class);
    private final ImageService imageService = mock(ImageService.class);
    private final IncidentService service =
            new IncidentService(incidentRepository, mock(UserRepository.class), imageService);

    private final Incident incident = Incident.builder().id("i1").photoFileIds(List.of("f1")).build();

    @Test
    void cargaUnaFotoQuePerteneceALaIncidencia() {
        GridFsResource resource = mock(GridFsResource.class);
        when(incidentRepository.findById("i1")).thenReturn(Optional.of(incident));
        when(imageService.load("f1")).thenReturn(resource);

        assertSame(resource, service.loadPhoto("i1", "f1"));
    }

    @Test
    void noSirveUnaFotoDeOtraIncidenciaAunqueSeConozcaSuId() {
        when(incidentRepository.findById("i1")).thenReturn(Optional.of(incident));

        ApiException ex = assertThrows(ApiException.class, () -> service.loadPhoto("i1", "f-de-otra"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(imageService, never()).load(any());
    }
}
