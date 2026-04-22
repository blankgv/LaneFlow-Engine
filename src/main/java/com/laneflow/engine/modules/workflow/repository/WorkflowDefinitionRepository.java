package com.laneflow.engine.modules.workflow.repository;

import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionRepository extends MongoRepository<WorkflowDefinition, String> {

    Optional<WorkflowDefinition> findByCode(String code);

    boolean existsByCode(String code);

    List<WorkflowDefinition> findByStatus(WorkflowStatus status);
}
