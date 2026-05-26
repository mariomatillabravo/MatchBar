package com.matchbar.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "reviews")
@CompoundIndex(name = "user_bar_unique", def = "{'userId': 1, 'barId': 1}", unique = true)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    private String id;

    private String userId;

    private String userName;

    private String barId;

    private Integer ratingAtmosphere;

    private Integer ratingFood;

    private Integer ratingPrice;

    private String comment;

    @CreatedDate
    private Instant createdAt;
}
