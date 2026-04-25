package com.laneflow.engine.modules.workflow.model;

import com.laneflow.engine.modules.workflow.model.enums.WorkflowAuditAction;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "workflow_audits")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowAudit {

    @Id
    private String id;

    @Indexed
    private String workflowDefinitionId;

    private String workflowCode;
    private String workflowName;
    private WorkflowAuditAction action;
    private String description;
    private String username;
    private WorkflowStatus statusBefore;
    private WorkflowStatus statusAfter;
    private Map<String, Object> metadata;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
