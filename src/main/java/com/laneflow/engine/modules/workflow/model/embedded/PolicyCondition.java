package com.laneflow.engine.modules.workflow.model.embedded;

import com.laneflow.engine.modules.workflow.model.enums.LogicalOperator;
import com.laneflow.engine.modules.workflow.model.enums.PolicyConditionOperator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyCondition {

    private String field;
    private PolicyConditionOperator operator;
    private String value;

    @Builder.Default
    private LogicalOperator logicalOperator = LogicalOperator.AND;
}
