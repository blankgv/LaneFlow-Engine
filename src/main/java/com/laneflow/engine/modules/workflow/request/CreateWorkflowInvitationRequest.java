package com.laneflow.engine.modules.workflow.request;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkflowInvitationRequest(@NotBlank String invitedUsername) {}
