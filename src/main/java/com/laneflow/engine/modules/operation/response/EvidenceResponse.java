package com.laneflow.engine.modules.operation.response;

import com.laneflow.engine.modules.operation.model.enums.EvidenceCategory;

import java.time.LocalDateTime;

public record EvidenceResponse(
        String id,
        String procedureId,
        String taskId,
        String nodeId,
        String formId,
        String fieldId,
        String fieldName,
        String uploadedBy,
        String fileName,
        String originalFileName,
        String contentType,
        String extension,
        long sizeBytes,
        String storageProvider,
        String bucketName,
        String storagePath,
        String mediaLink,
        String description,
        EvidenceCategory category,
        LocalDateTime createdAt
) {}
