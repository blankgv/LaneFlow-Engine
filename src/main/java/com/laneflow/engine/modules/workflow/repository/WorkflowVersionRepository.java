package com.laneflow.engine.modules.workflow.repository;

import com.laneflow.engine.modules.workflow.model.WorkflowVersion;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowVersionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowVersionRepository extends MongoRepository<WorkflowVersion, String> {

    List<WorkflowVersion> findByWorkflowDefinitionIdOrderByVersionNumberDesc(String workflowDefinitionId);

    Optional<WorkflowVersion> findByWorkflowDefinitionIdAndVersionNumber(String workflowDefinitionId, int versionNumber);

    Optional<WorkflowVersion> findByWorkflowDefinitionIdAndStatus(String workflowDefinitionId, WorkflowVersionStatus status);
}
