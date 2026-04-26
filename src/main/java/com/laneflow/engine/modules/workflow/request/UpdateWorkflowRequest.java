package com.laneflow.engine.modules.workflow.request;

import com.laneflow.engine.modules.workflow.model.enums.NodeType;

import java.util.List;

public record UpdateWorkflowRequest(
        String name,
        String description,
        String bpmnXml,
        List<SwimlaneRequest> swimlanes,
        List<NodeRequest> nodes,
        List<TransitionRequest> transitions
) {

    public record SwimlaneRequest(
            String id,
            String name,
            String departmentId,
            String departmentCode
    ) {}

    public record NodeRequest(
            String id,
            String name,
            NodeType type,
            String swimlaneId,
            String departmentId,
            String formKey,
            String requiredAction
    ) {}

    public record TransitionRequest(
            String id,
            String sourceNodeId,
            String targetNodeId,
            String condition,
            String label
    ) {}
}
