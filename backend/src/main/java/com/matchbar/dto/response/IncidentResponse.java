package com.matchbar.dto.response;

import com.matchbar.entity.Incident;
import java.time.Instant;
import java.util.List;

public record IncidentResponse(
        String id,
        String senderName,
        String senderEmail,
        String senderType,
        String subject,
        String message,
        String status,
        Instant createdAt,
        Instant resolvedAt,
        List<String> photoUrls
) {
    public static IncidentResponse from(Incident i) {
        // Endpoint privado (solo ADMIN): estas fotos pueden contener datos personales.
        String base = "/api/admin/incidents/" + i.getId() + "/photos/";
        List<String> photos = (i.getPhotoFileIds() == null) ? List.of()
                : i.getPhotoFileIds().stream().map(id -> base + id).toList();
        return new IncidentResponse(
                i.getId(),
                i.getSenderName(),
                i.getSenderEmail(),
                i.getSenderType().name(),
                i.getSubject(),
                i.getMessage(),
                i.getStatus().name(),
                i.getCreatedAt(),
                i.getResolvedAt(),
                photos
        );
    }
}
