package com.laneflow.engine.modules.workflow.response;

import com.laneflow.engine.modules.workflow.model.enums.WorkflowInvitationStatus;

import java.time.LocalDateTime;

public record WorkflowInvitationResponse(
        String id,
        String workflowDefinitionId,
        String invitedUserId,
        String invitedUsername,
        String invitedUserEmail,
        String invitedByUserId,
        String invitedByUsername,
        WorkflowInvitationStatus status,
        LocalDateTime createdAt,
        LocalDateTime respondedAt
) {}
