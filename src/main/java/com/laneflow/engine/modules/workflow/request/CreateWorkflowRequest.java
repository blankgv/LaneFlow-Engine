package com.laneflow.engine.modules.workflow.request;

import com.laneflow.engine.modules.workflow.model.enums.NodeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateWorkflowRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        List<SwimlaneRequest> swimlanes,
        List<NodeRequest> nodes,
        List<TransitionRequest> transitions
) {

    public record SwimlaneRequest(
            String id,
            @NotBlank String name,
            String departmentId,
            String departmentCode
    ) {}

    public record NodeRequest(
            String id,
            @NotBlank String name,
            @NotNull NodeType type,
            String swimlaneId,
            String departmentId,
            String formKey,
            String requiredAction
    ) {}

    public record TransitionRequest(
            String id,
            @NotBlank String sourceNodeId,
            @NotBlank String targetNodeId,
            String condition,
            String label
    ) {}
}
