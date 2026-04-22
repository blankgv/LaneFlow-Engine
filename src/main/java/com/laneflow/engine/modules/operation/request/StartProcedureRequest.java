package com.laneflow.engine.modules.operation.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record StartProcedureRequest(
        @NotBlank String workflowDefinitionId,
        @NotBlank String applicantId,
        Map<String, Object> formData
) {}
