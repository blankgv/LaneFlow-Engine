package com.laneflow.engine.modules.workflow.request;

import com.laneflow.engine.modules.workflow.model.enums.LogicalOperator;
import com.laneflow.engine.modules.workflow.model.enums.PolicyActionType;
import com.laneflow.engine.modules.workflow.model.enums.PolicyConditionOperator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreatePolicyRequest(
        @NotBlank String name,
        String description,
        @NotBlank String workflowDefinitionId,
        @NotBlank String nodeId,
        int priority,
        @NotEmpty List<PolicyConditionRequest> conditions,
        @NotEmpty List<PolicyActionRequest> actions
) {

    public record PolicyConditionRequest(
            @NotBlank String field,
            @NotNull PolicyConditionOperator operator,
            String value,
            LogicalOperator logicalOperator
    ) {}

    public record PolicyActionRequest(
            @NotNull PolicyActionType type,
            String targetNodeId,
            String targetNodeName,
            String targetDepartmentId,
            String variableName,
            String variableValue,
            String notificationMessage
    ) {}
}
