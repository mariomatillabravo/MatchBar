package com.matchbar.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "competitions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Competition {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String externalId;

    private String name;

    private String country;

    private String logoUrl;
}
