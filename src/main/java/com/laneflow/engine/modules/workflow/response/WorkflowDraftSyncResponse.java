package com.laneflow.engine.modules.workflow.response;

import java.time.LocalDateTime;

public record WorkflowDraftSyncResponse(
        String workflowId,
        String eventType,
        String bpmnXml,
        String lastModifiedBy,
        LocalDateTime updatedAt
) {
}
