package com.matchbar.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "broadcasts",
       uniqueConstraints = @UniqueConstraint(columnNames = {"bar_id", "match_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Broadcast {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bar_id", nullable = false)
    private Bar bar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;
}
