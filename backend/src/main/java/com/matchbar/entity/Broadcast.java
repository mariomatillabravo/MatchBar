package com.matchbar.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "broadcasts")
@CompoundIndex(name = "bar_match_unique", def = "{'barId': 1, 'matchId': 1}", unique = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Broadcast {

    @Id
    private String id;

    private String barId;

    private String matchId;
}
