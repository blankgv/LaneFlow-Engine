package com.laneflow.engine.modules.workflow.repository;

import com.laneflow.engine.modules.workflow.model.WorkflowCollaborator;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowCollaboratorRepository extends MongoRepository<WorkflowCollaborator, String> {

    List<WorkflowCollaborator> findByWorkflowDefinitionIdOrderByCreatedAtAsc(String workflowDefinitionId);

    Optional<WorkflowCollaborator> findByWorkflowDefinitionIdAndUserId(String workflowDefinitionId, String userId);

    boolean existsByWorkflowDefinitionIdAndUserId(String workflowDefinitionId, String userId);
}
