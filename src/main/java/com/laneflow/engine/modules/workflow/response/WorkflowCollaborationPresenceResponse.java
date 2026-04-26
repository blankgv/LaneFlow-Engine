package com.laneflow.engine.modules.workflow.response;

import java.time.LocalDateTime;
import java.util.List;

public record WorkflowCollaborationPresenceResponse(
        String workflowId,
        String action,
        String username,
        int activeCount,
        List<String> activeUsers,
        LocalDateTime occurredAt
) {
}
