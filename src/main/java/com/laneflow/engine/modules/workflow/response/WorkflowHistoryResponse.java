package com.laneflow.engine.modules.workflow.response;

import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;

import java.util.List;

public record WorkflowHistoryResponse(
        String workflowId,
        String workflowCode,
        String workflowName,
        WorkflowStatus currentStatus,
        Integer currentVersion,
        List<WorkflowHistoryItemResponse> history
) {
}
