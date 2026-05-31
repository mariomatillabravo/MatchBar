package com.matchbar.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "incidents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Incident {

    @Id
    private String id;

    private String userId;

    private String senderName;

    private String senderEmail;

    private User.Role senderType;

    private String subject;

    private String message;

    /** Fotos adjuntas a la incidencia (ids de ficheros en GridFS). */
    @Builder.Default
    private List<String> photoFileIds = new ArrayList<>();

    @Builder.Default
    private Status status = Status.OPEN;

    @CreatedDate
    private Instant createdAt;

    private Instant resolvedAt;

    public enum Status { OPEN, RESOLVED }
}
