package com.laneflow.engine.modules.workflow.request;

import jakarta.validation.constraints.NotBlank;

public record CreateFormRequest(
        @NotBlank String workflowDefinitionId,
        @NotBlank String nodeId,
        @NotBlank String nodeName,
        @NotBlank String title
) {}
