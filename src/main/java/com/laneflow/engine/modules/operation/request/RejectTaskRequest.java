package com.laneflow.engine.modules.operation.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record RejectTaskRequest(
        @NotBlank String comment,
        Map<String, Object> formData
) {}
