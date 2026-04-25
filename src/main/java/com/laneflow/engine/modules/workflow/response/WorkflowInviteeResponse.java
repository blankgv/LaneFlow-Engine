package com.laneflow.engine.modules.workflow.response;

public record WorkflowInviteeResponse(
        String userId,
        String username,
        String email,
        String roleId,
        String roleCode,
        String roleName
) {}
