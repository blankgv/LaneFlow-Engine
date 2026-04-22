package com.laneflow.engine.modules.workflow.request;

import com.laneflow.engine.modules.workflow.model.enums.FieldType;

import java.util.List;

public record UpdateFieldRequest(
        String name,
        String label,
        FieldType type,
        Boolean required,
        Integer order,
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
