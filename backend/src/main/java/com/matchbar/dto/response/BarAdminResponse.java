package com.matchbar.dto.response;

import com.matchbar.entity.Bar;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record BarAdminResponse(
        String id,
        String userId,
        String name,
        String description,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String ownerPhone,
        List<String> photoUrls,
        List<String> menuUrls,
        String licenseDocFilename,
        Bar.Status status,
        String ownerName,
        String ownerEmail,
        Double averageRating,
        Instant createdAt
) {}
