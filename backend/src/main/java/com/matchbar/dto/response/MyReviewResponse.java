package com.matchbar.dto.response;

import java.time.Instant;

/** Reseña creada por el usuario, incluyendo el bar al que pertenece. */
public record MyReviewResponse(
        String id,
        String barId,
        String barName,
        Integer ratingAtmosphere,
        Integer ratingFood,
        Integer ratingPrice,
        Double average,
        String comment,
        Instant createdAt
) {}
