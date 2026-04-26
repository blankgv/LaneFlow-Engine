package com.laneflow.engine.modules.workflow.repository;

import com.laneflow.engine.modules.workflow.model.WorkflowInvitation;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowInvitationStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowInvitationRepository extends MongoRepository<WorkflowInvitation, String> {

    List<WorkflowInvitation> findByWorkflowDefinitionIdOrderByCreatedAtDesc(String workflowDefinitionId);

    List<WorkflowInvitation> findByInvitedUserIdOrderByCreatedAtDesc(String invitedUserId);

    Optional<WorkflowInvitation> findByWorkflowDefinitionIdAndInvitedUserIdAndStatus(
            String workflowDefinitionId,
            String invitedUserId,
            WorkflowInvitationStatus status
    );
}
