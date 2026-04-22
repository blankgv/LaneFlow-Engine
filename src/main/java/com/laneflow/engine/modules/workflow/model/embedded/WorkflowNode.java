package com.laneflow.engine.modules.workflow.model.embedded;

import com.laneflow.engine.modules.workflow.model.enums.NodeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowNode {

    private String id;
    private String name;
    private NodeType type;
    private String swimlaneId;
    private String departmentId;
    private String formKey;
    private String requiredAction;
}
