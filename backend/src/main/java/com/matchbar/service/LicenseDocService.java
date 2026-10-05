package com.matchbar.service;

import com.matchbar.exception.ApiException;
import com.matchbar.util.FileSignatures;
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

@Service
@RequiredArgsConstructor
public class LicenseDocService {

    private final GridFsTemplate gridFsTemplate;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fichero vacío");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No se ha podido leer el fichero");
        }
        // Comprobamos la firma "%PDF-" del contenido: el Content-Type lo elige el cliente.
        if (!FileSignatures.isPdf(data)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El fichero debe ser un PDF");
        }
        return store(data, file.getOriginalFilename());
    }

    public String store(byte[] data, String filename) {
        ObjectId id = gridFsTemplate.store(new java.io.ByteArrayInputStream(data), filename, "application/pdf");
        return id.toHexString();
    }

    public GridFsResource load(String fileId) {
        GridFSFile file = gridFsTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        if (file == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Licencia no encontrada");
        }
        return gridFsTemplate.getResource(file);
    }

    public void delete(String fileId) {
        if (fileId == null) return;
        try {
            gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(fileId))));
        } catch (IllegalArgumentException ignored) {
            // fileId con formato inválido — nada que borrar
        }
    }
}
