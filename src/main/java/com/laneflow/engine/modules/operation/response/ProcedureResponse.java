package com.laneflow.engine.modules.operation.response;

import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;

import java.time.LocalDateTime;
import java.util.Map;

public record ProcedureResponse(
        String id,
        String code,
        String workflowDefinitionId,
        String workflowCode,
        String workflowName,
        int workflowVersion,
        String camundaProcessKey,
        String camundaProcessInstanceId,
        String previousCamundaProcessInstanceId,
        String applicantId,
        String applicantDocumentNumber,
        String applicantName,
        ProcedureStatus status,
        String currentTaskId,
        String currentNodeId,
        String currentNodeName,
        String currentAssigneeUsername,
        LocalDateTime claimedAt,
        Map<String, Object> formData,
        String lastAction,
        String lastComment,
        String lastCompletedTaskId,
        String lastCompletedTaskName,
        String lastCompletedBy,
        LocalDateTime lastCompletedAt,
        int resubmissionCount,
        String resolvedObservationBy,
        LocalDateTime resolvedObservationAt,
        String resolvedObservationComment,
        String startedBy,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
