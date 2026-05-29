package com.matchbar.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "matches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Match {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String externalId;

    @DBRef
    private Competition competition;

    @DBRef
    private Team homeTeam;

    @DBRef
    private Team awayTeam;

    private Instant kickoffAt;

    private String status;
}
