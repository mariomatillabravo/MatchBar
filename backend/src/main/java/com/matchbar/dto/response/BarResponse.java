package com.matchbar.dto.response;

import com.matchbar.entity.Bar;
import java.math.BigDecimal;

public record BarResponse(
        Long id,
        String name,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String photoUrl,
        Bar.Status status,
        Double distanceMeters,
        Double averageRating
) {
    public static BarResponse from(Bar b) {
        return new BarResponse(b.getId(), b.getName(), b.getDescription(),
                b.getAddress(), b.getLatitude(), b.getLongitude(),
                b.getPhotoUrl(), b.getStatus(), null, null);
    }

    public static BarResponse from(Bar b, Double distance, Double avgRating) {
        return new BarResponse(b.getId(), b.getName(), b.getDescription(),
                b.getAddress(), b.getLatitude(), b.getLongitude(),
                b.getPhotoUrl(), b.getStatus(), distance, avgRating);
    }
}
