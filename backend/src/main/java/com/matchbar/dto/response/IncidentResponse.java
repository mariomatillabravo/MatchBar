package com.matchbar.dto.response;

import com.matchbar.entity.Incident;
import java.time.Instant;

public record IncidentResponse(
        String id,
        String senderName,
        String senderEmail,
        String senderType,
        String subject,
        String message,
        String status,
        Instant createdAt,
        Instant resolvedAt
) {
    public static IncidentResponse from(Incident i) {
        return new IncidentResponse(
                i.getId(),
                i.getSenderName(),
                i.getSenderEmail(),
                i.getSenderType().name(),
                i.getSubject(),
                i.getMessage(),
                i.getStatus().name(),
                i.getCreatedAt(),
                i.getResolvedAt()
        );
    }
}
