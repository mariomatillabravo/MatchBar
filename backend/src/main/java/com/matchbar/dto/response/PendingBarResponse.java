package com.matchbar.dto.response;

import java.util.List;

public record PendingBarResponse(
        String id,
        String name,
        String description,
        String address,
        String ownerName,
        String ownerEmail,
        String ownerPhone,
        String licenseDocFilename,
        String licenseDocUrl,
        List<String> photoUrls,
        List<String> menuUrls
) {}
