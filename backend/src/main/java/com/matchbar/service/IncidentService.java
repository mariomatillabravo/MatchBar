package com.matchbar.service;

import com.matchbar.dto.request.IncidentRequest;
import com.matchbar.dto.response.IncidentResponse;
import com.matchbar.entity.Incident;
import com.matchbar.entity.User;
import com.matchbar.exception.ApiException;
import com.matchbar.repository.IncidentRepository;
import com.matchbar.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    public IncidentResponse create(String userEmail, IncidentRequest req) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Incident incident = Incident.builder()
                .userId(user.getId())
                .senderName(user.getName())
                .senderEmail(user.getEmail())
                .senderType(user.getRole())
                .subject(req.subject())
                .message(req.message())
                .build();
        return IncidentResponse.from(incidentRepository.save(incident));
    }

    public List<IncidentResponse> listAll() {
        return incidentRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(IncidentResponse::from).toList();
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
