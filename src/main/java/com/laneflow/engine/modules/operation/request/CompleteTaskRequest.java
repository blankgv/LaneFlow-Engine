package com.laneflow.engine.modules.operation.request;

import com.laneflow.engine.modules.operation.model.enums.TaskAction;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CompleteTaskRequest(
        @NotNull TaskAction action,
        String comment,
        Map<String, Object> formData
) {}
