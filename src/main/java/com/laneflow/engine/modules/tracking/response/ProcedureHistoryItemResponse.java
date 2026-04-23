package com.laneflow.engine.modules.tracking.response;

import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import com.laneflow.engine.modules.tracking.model.enums.ProcedureAuditAction;

import java.time.LocalDateTime;
import java.util.Map;

public record ProcedureHistoryItemResponse(
        String id,
        ProcedureAuditAction action,
        String title,
        String message,
        String username,
        String taskId,
        String nodeId,
        String nodeName,
        ProcedureStatus statusBefore,
        ProcedureStatus statusAfter,
        Map<String, Object> metadata,
        LocalDateTime createdAt
) {
}
