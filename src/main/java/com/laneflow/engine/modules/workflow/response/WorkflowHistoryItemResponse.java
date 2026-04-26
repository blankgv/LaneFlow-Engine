package com.laneflow.engine.modules.workflow.response;

import com.laneflow.engine.modules.workflow.model.enums.WorkflowAuditAction;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;

import java.time.LocalDateTime;
import java.util.Map;

public record WorkflowHistoryItemResponse(
        String id,
        WorkflowAuditAction action,
        String title,
        String message,
        String username,
        WorkflowStatus statusBefore,
        WorkflowStatus statusAfter,
        Map<String, Object> metadata,
        LocalDateTime createdAt
) {
}
