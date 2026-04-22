package com.laneflow.engine.modules.workflow.response;

import com.laneflow.engine.modules.workflow.model.embedded.FieldValidation;
import com.laneflow.engine.modules.workflow.model.embedded.FileConfig;
import com.laneflow.engine.modules.workflow.model.enums.FieldType;

import java.time.LocalDateTime;
import java.util.List;

public record FormFieldResponse(
        String id,
        String formId,
        String name,
        String label,
        FieldType type,
        boolean required,
        int order,
        List<String> options,
        List<FieldValidation> validations,
        FileConfig fileConfig,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
