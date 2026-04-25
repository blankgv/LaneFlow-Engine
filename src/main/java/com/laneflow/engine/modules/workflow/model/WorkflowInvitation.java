package com.laneflow.engine.modules.workflow.model;

import com.laneflow.engine.modules.workflow.model.enums.WorkflowInvitationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "workflow_invitations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInvitation {

    @Id
    private String id;

    @Indexed
    private String workflowDefinitionId;

    @Indexed
    private String invitedUserId;

    private String invitedByUserId;

    @Builder.Default
    private WorkflowInvitationStatus status = WorkflowInvitationStatus.PENDING;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime respondedAt;
}
