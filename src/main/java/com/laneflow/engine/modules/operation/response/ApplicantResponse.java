package com.laneflow.engine.modules.operation.response;

import com.laneflow.engine.modules.operation.model.enums.ApplicantType;
import com.laneflow.engine.modules.operation.model.enums.DocumentType;

import java.time.LocalDateTime;

public record ApplicantResponse(
        String id,
        ApplicantType type,
        DocumentType documentType,
        String documentNumber,
        String firstName,
        String lastName,
        String businessName,
        String legalRepresentative,
        String email,
        String phone,
        String address,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
