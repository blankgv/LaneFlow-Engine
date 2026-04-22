package com.laneflow.engine.modules.workflow.repository;

import com.laneflow.engine.modules.workflow.model.DynamicForm;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DynamicFormRepository extends MongoRepository<DynamicForm, String> {

    List<DynamicForm> findByWorkflowDefinitionId(String workflowDefinitionId);

    Optional<DynamicForm> findByWorkflowDefinitionIdAndNodeId(String workflowDefinitionId, String nodeId);
}
