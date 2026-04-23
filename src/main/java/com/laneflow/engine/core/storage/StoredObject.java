package com.laneflow.engine.core.storage;

public record StoredObject(
        String bucketName,
        String objectName,
        String contentType,
        long sizeBytes,
        String mediaLink
) {}
