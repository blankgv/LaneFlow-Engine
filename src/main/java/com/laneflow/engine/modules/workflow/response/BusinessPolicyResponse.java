package com.laneflow.engine.modules.workflow.response;

import com.laneflow.engine.modules.workflow.model.embedded.PolicyAction;
import com.laneflow.engine.modules.workflow.model.embedded.PolicyCondition;

import java.time.LocalDateTime;
import java.util.List;

public record BusinessPolicyResponse(
        String id,
        String name,
        String description,
        String workflowDefinitionId,
        String nodeId,
        boolean active,
        int priority,
        List<PolicyCondition> conditions,
        List<PolicyAction> actions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
