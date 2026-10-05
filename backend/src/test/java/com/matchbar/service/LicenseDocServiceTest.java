package com.matchbar.service;

import com.matchbar.exception.ApiException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LicenseDocServiceTest {

    private final GridFsTemplate gridFs = mock(GridFsTemplate.class);
    private final LicenseDocService service = new LicenseDocService(gridFs);

    @Test
    void rechazaUnFicheroQueSoloDiceSerPdf() {
        MockMultipartFile fake = new MockMultipartFile("file", "licencia.pdf", "application/pdf",
                "MZ ejecutable disfrazado".getBytes(StandardCharsets.US_ASCII));

        ApiException ex = assertThrows(ApiException.class, () -> service.store(fake));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(gridFs);
    }

    @Test
    void aceptaUnPdfAunqueElClienteNoIndiqueElTipo() {
        ObjectId id = new ObjectId();
        when(gridFs.store(any(InputStream.class), eq("licencia.pdf"), eq("application/pdf"))).thenReturn(id);
        MockMultipartFile pdf = new MockMultipartFile("file", "licencia.pdf", "application/octet-stream",
                "%PDF-1.4\n...".getBytes(StandardCharsets.US_ASCII));

        assertEquals(id.toHexString(), service.store(pdf));
    }
}
