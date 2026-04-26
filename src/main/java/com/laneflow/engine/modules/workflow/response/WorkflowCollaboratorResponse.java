package com.laneflow.engine.modules.workflow.response;

import java.time.LocalDateTime;

public record WorkflowCollaboratorResponse(
        String id,
        String workflowDefinitionId,
        String userId,
        String username,
        String email,
        String addedBy,
        LocalDateTime createdAt
) {}
