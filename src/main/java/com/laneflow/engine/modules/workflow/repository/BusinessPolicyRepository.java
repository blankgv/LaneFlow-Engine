package com.laneflow.engine.modules.workflow.repository;

import com.laneflow.engine.modules.workflow.model.BusinessPolicy;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BusinessPolicyRepository extends MongoRepository<BusinessPolicy, String> {

    List<BusinessPolicy> findByWorkflowDefinitionId(String workflowDefinitionId);

    List<BusinessPolicy> findByWorkflowDefinitionIdAndActiveTrue(String workflowDefinitionId);

    List<BusinessPolicy> findByWorkflowDefinitionIdAndNodeId(String workflowDefinitionId, String nodeId);
}
