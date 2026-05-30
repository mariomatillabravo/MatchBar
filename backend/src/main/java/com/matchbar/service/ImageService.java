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

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/** Almacena y sirve imágenes (fotos del bar y carta) usando GridFS. */
@Service
@RequiredArgsConstructor
public class ImageService {

    private static final Set<String> ALLOWED = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");

    private final GridFsTemplate gridFsTemplate;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Imagen vacía");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED.contains(contentType.toLowerCase())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La imagen debe ser JPG, PNG o WEBP");
        }
        try (InputStream is = file.getInputStream()) {
            ObjectId id = gridFsTemplate.store(is, file.getOriginalFilename(), contentType);
            return id.toHexString();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al almacenar la imagen");
        }
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
