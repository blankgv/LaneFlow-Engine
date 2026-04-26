package com.laneflow.engine.modules.workflow.request;

import jakarta.validation.constraints.NotBlank;

public record WorkflowDraftSyncRequest(
        String username,
        @NotBlank String bpmnXml
) {
}
