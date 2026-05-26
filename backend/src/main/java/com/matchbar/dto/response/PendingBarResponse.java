package com.matchbar.dto.response;

public record PendingBarResponse(
        String id,
        String name,
        String address,
        String ownerName,
        String ownerEmail,
        String ownerPhone,
        String licenseDocFilename,
        String licenseDocUrl
) {}
