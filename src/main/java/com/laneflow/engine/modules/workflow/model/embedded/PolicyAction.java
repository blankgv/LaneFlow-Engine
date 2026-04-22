package com.laneflow.engine.modules.workflow.model.embedded;

import com.laneflow.engine.modules.workflow.model.enums.PolicyActionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyAction {

    private PolicyActionType type;
    private String targetNodeId;
    private String targetNodeName;
    private String targetDepartmentId;
    private String variableName;
    private String variableValue;
    private String notificationMessage;
}
