package com.laneflow.engine.modules.workflow.request;

import jakarta.validation.constraints.NotBlank;

public record WorkflowCollaborationPresenceRequest(
        @NotBlank String username
) {
}
