package com.laneflow.engine.modules.tracking.response;

import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;

import java.time.LocalDateTime;

public record ProcedureStatusResponse(
        String procedureId,
        String procedureCode,
        String workflowName,
        String applicantName,
        ProcedureStatus currentStatus,
        String currentStage,
        String statusMessage,
        String currentAssigneeUsername,
        String lastEventTitle,
        String lastEventMessage,
        LocalDateTime startedAt,
        LocalDateTime lastUpdatedAt,
        LocalDateTime completedAt
) {
}
