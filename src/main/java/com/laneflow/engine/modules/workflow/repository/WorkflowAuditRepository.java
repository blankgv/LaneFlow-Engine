package com.laneflow.engine.modules.workflow.repository;

import com.laneflow.engine.modules.workflow.model.WorkflowAudit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorkflowAuditRepository extends MongoRepository<WorkflowAudit, String> {

    List<WorkflowAudit> findByWorkflowDefinitionIdOrderByCreatedAtAsc(String workflowDefinitionId);
}
