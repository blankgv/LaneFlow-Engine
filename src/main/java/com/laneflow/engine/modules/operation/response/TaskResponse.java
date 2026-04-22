package com.laneflow.engine.modules.operation.response;

import java.time.LocalDateTime;

public record TaskResponse(
        String id,
        String name,
        String taskDefinitionKey,
        String processInstanceId,
        String procedureId,
        String procedureCode,
        String workflowDefinitionId,
        String workflowName,
        String applicantId,
        String applicantName,
        String assignee,
        String responsibleDepartmentId,
        String responsibleDepartmentCode,
        LocalDateTime createdAt
) {}
