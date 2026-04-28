package com.laneflow.engine.modules.operation.response;

import com.laneflow.engine.modules.workflow.response.DynamicFormResponse;

import java.time.LocalDateTime;
import java.util.Map;

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
        String responsibleDepartmentName,
        Map<String, Object> formData,
        DynamicFormResponse form,
        LocalDateTime createdAt
) {}
