package com.matchbar.dto.response;

import com.matchbar.entity.Bar;
import java.math.BigDecimal;
import java.util.List;

public record BarResponse(
        String id,
        String name,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String photoUrl,
        List<String> photoUrls,
        String menuUrl,
        String ownerPhone,
        Bar.Status status,
        Double distanceMeters,
        Double averageRating
) {
    private static final String IMG_PATH = "/api/bars/images/";

    public static BarResponse from(Bar b) {
        return from(b, null, null);
    }

    public static BarResponse from(Bar b, Double distance, Double avgRating) {
        List<String> photos = (b.getPhotoFileIds() == null) ? List.of()
                : b.getPhotoFileIds().stream().map(id -> IMG_PATH + id).toList();
        String menu = b.getMenuFileId() != null ? IMG_PATH + b.getMenuFileId() : null;
        return new BarResponse(b.getId(), b.getName(), b.getDescription(),
                b.getAddress(), b.getLatitude(), b.getLongitude(),
                b.getPhotoUrl(), photos, menu, b.getOwnerPhone(),
                b.getStatus(), distance, avgRating);
    }
}
