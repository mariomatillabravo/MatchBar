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
    private static final String IMG_PATH = "/api/bars/images/";

    public static IncidentResponse from(Incident i) {
        List<String> photos = (i.getPhotoFileIds() == null) ? List.of()
                : i.getPhotoFileIds().stream().map(id -> IMG_PATH + id).toList();
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
