package com.laneflow.engine.modules.workflow.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowTransition {

    private String id;
    private String sourceNodeId;
    private String targetNodeId;
    private String condition;
    private String label;
}
