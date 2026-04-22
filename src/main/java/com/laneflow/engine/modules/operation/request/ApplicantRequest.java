package com.laneflow.engine.modules.operation.request;

import com.laneflow.engine.modules.operation.model.enums.ApplicantType;
import com.laneflow.engine.modules.operation.model.enums.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplicantRequest(
        @NotNull ApplicantType type,
        @NotNull DocumentType documentType,
        @NotBlank String documentNumber,
        String firstName,
        String lastName,
        String businessName,
        String legalRepresentative,
        @Email String email,
        String phone,
        String address
) {}
