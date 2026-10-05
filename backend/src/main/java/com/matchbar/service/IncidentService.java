package com.matchbar.service;

import com.matchbar.dto.response.IncidentResponse;
import com.matchbar.entity.Incident;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.IncidentRepository;
import com.matchbar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;

    public IncidentResponse create(String userEmail, String subject, String message, List<MultipartFile> photos) {
        if (subject == null || subject.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El asunto es obligatorio");
        }
        if (message == null || message.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La descripción es obligatoria");
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<String> photoIds = new ArrayList<>();
        if (photos != null) {
            for (MultipartFile photo : photos) {
                if (photo != null && !photo.isEmpty()) {
                    photoIds.add(imageService.store(photo));
                }
            }
        }

        Incident incident = Incident.builder()
                .userId(user.getId())
                .senderName(user.getName())
                .senderEmail(user.getEmail())
                .senderType(user.getRole())
                .subject(subject.trim())
                .message(message.trim())
                .photoFileIds(photoIds)
                .build();
        return IncidentResponse.from(incidentRepository.save(incident));
    }

    public List<IncidentResponse> listAll() {
        return incidentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(IncidentResponse::from).toList();
    }

    /** Foto adjunta a una incidencia (solo para el panel de administración). */
    public GridFsResource loadPhoto(String incidentId, String fileId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Incidencia no encontrada"));
        if (incident.getPhotoFileIds() == null || !incident.getPhotoFileIds().contains(fileId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Imagen no encontrada");
        }
        return imageService.load(fileId);
    }

    public IncidentResponse resolve(String id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Incidencia no encontrada"));
        incident.setStatus(Incident.Status.RESOLVED);
        incident.setResolvedAt(Instant.now());
        return IncidentResponse.from(incidentRepository.save(incident));
    }

    public void delete(String id) {
        if (!incidentRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Incidencia no encontrada");
        }
        incidentRepository.deleteById(id);
    }
}
