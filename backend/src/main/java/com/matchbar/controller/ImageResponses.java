package com.matchbar.controller;

import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

/** Respuesta HTTP para una imagen guardada en GridFS. */
final class ImageResponses {

    private ImageResponses() {}

    static ResponseEntity<InputStreamResource> of(GridFsResource resource) throws IOException {
        String contentType = resource.getContentType();
        MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(new InputStreamResource(resource.getInputStream()));
    }
}
