package com.laneflow.engine.modules.workflow.request;

import com.laneflow.engine.modules.workflow.model.enums.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateFieldRequest(
        @NotBlank String name,
        @NotBlank String label,
        @NotNull FieldType type,
        boolean required,
        int order,
        List<String> options,
        List<FieldValidationRequest> validations,
        FileConfigRequest fileConfig
) {

    public record FieldValidationRequest(
            String type,
            String value,
            String message
    ) {}

    public record FileConfigRequest(
            List<String> allowedExtensions,
            int maxSizeMb,
            boolean multiple,
            String bucketFolder
    ) {}
}
