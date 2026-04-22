package com.laneflow.engine.modules.workflow.model;

import com.laneflow.engine.modules.workflow.model.embedded.Swimlane;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowTransition;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "workflow_definitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowDefinition {

    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private String name;
    private String description;

    @Builder.Default
    private WorkflowStatus status = WorkflowStatus.DRAFT;

    @Builder.Default
    private int currentVersion = 1;

    private String camundaProcessKey;
    private String camundaDeploymentId;
    private String camundaProcessDefinitionId;

    private List<Swimlane> swimlanes;
    private List<WorkflowNode> nodes;
    private List<WorkflowTransition> transitions;

    private String createdBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
}
