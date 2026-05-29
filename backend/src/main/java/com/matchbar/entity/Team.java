package com.matchbar.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "teams")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Team {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String externalId;

    private String name;

    private String logoUrl;

    private String competitionId;
}
