package com.laneflow.engine.modules.workflow.model;

import com.laneflow.engine.modules.workflow.model.enums.WorkflowVersionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "workflow_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowVersion {

    @Id
    private String id;

    @Indexed
    private String workflowDefinitionId;

    private int versionNumber;
    private String bpmnXml;

    @Builder.Default
    private WorkflowVersionStatus status = WorkflowVersionStatus.DRAFT;

    private String camundaDeploymentId;
    private String camundaProcessDefinitionId;

    private Object snapshot;

    private String createdBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime publishedAt;
}
