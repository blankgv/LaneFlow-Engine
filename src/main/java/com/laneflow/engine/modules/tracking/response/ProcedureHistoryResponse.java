package com.laneflow.engine.modules.tracking.response;

import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ProcedureHistoryResponse(
        String procedureId,
        String procedureCode,
        String workflowName,
        String applicantName,
        ProcedureStatus currentStatus,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        List<ProcedureHistoryItemResponse> history
) {
}
