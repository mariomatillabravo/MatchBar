package com.matchbar.service;

import com.matchbar.exception.ApiException;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Almacena y sirve imágenes (fotos del bar, carta e incidencias) usando
 * GridFS. Todo lo que se guarda pasa antes por {@link ImageSanitizer}.
 */
@Service
@RequiredArgsConstructor
public class ImageService {

    private final GridFsTemplate gridFsTemplate;
    private final ImageSanitizer imageSanitizer;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Imagen vacía");
        }
        byte[] original;
        try {
            original = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se ha podido leer la imagen");
        }
        // El tipo lo decide el contenido, no el Content-Type que declara el cliente.
        ImageSanitizer.SanitizedImage clean = imageSanitizer.sanitize(original);
        ObjectId id = gridFsTemplate.store(new ByteArrayInputStream(clean.data()),
                file.getOriginalFilename(), clean.contentType());
        return id.toHexString();
    }

    public GridFsResource load(String fileId) {
        GridFSFile file;
        try {
            file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Imagen no encontrada");
        }
        if (file == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Imagen no encontrada");
        }
        return gridFsTemplate.getResource(file);
    }

    public void delete(String fileId) {
        if (fileId == null) return;
        try {
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        } catch (IllegalArgumentException ignored) {
            // id inválido — nada que borrar
        }
    }
}
