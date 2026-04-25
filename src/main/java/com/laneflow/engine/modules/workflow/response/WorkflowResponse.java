package com.laneflow.engine.modules.workflow.response;

import com.laneflow.engine.modules.workflow.model.embedded.Swimlane;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowTransition;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;

import java.time.LocalDateTime;
import java.util.List;

public record WorkflowResponse(
        String id,
        String code,
        String name,
        String description,
        WorkflowStatus status,
        int currentVersion,
        String camundaProcessKey,
        String draftBpmnXml,
        Integer publishedVersionNumber,
        List<Swimlane> swimlanes,
        List<WorkflowNode> nodes,
        List<WorkflowTransition> transitions,
        String createdBy,
        String lastModifiedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime publishedAt
) {}
