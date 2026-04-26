package com.laneflow.engine.modules.workflow.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "workflow_collaborators")
@CompoundIndex(def = "{'workflowDefinitionId': 1, 'userId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowCollaborator {

    @Id
    private String id;

    private String workflowDefinitionId;
    private String userId;
    private String addedBy;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
