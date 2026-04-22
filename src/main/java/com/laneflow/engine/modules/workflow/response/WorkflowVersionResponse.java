package com.laneflow.engine.modules.workflow.response;

import com.laneflow.engine.modules.workflow.model.enums.WorkflowVersionStatus;

import java.time.LocalDateTime;

public record WorkflowVersionResponse(
        String id,
        String workflowDefinitionId,
        int versionNumber,
        String bpmnXml,
        WorkflowVersionStatus status,
        String camundaDeploymentId,
        LocalDateTime createdAt,
        LocalDateTime publishedAt
) {}
