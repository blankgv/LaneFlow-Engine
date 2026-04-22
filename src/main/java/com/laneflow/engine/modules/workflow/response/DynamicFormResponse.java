package com.laneflow.engine.modules.workflow.response;

import java.time.LocalDateTime;
import java.util.List;

public record DynamicFormResponse(
        String id,
        String workflowDefinitionId,
        String nodeId,
        String nodeName,
        String title,
        List<FormFieldResponse> fields,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
