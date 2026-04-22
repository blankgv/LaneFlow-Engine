package com.laneflow.engine.modules.workflow.request;

import com.laneflow.engine.modules.workflow.model.enums.LogicalOperator;
import com.laneflow.engine.modules.workflow.model.enums.PolicyActionType;
import com.laneflow.engine.modules.workflow.model.enums.PolicyConditionOperator;

import java.util.List;

public record UpdatePolicyRequest(
        String name,
        String description,
        String workflowDefinitionId,
        String nodeId,
        Integer priority,
        List<PolicyConditionRequest> conditions,
        List<PolicyActionRequest> actions
) {

    public record PolicyConditionRequest(
            String field,
            PolicyConditionOperator operator,
            String value,
            LogicalOperator logicalOperator
    ) {}

    public record PolicyActionRequest(
            PolicyActionType type,
            String targetNodeId,
            String targetNodeName,
            String targetDepartmentId,
            String variableName,
            String variableValue,
            String notificationMessage
    ) {}
}
