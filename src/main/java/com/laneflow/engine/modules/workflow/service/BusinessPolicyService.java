package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.request.CreatePolicyRequest;
import com.laneflow.engine.modules.workflow.request.UpdatePolicyRequest;
import com.laneflow.engine.modules.workflow.response.BusinessPolicyResponse;

import java.util.List;

public interface BusinessPolicyService {

    List<BusinessPolicyResponse> findByWorkflow(String workflowId);

    BusinessPolicyResponse findById(String id);

    BusinessPolicyResponse create(CreatePolicyRequest request);

    BusinessPolicyResponse update(String id, UpdatePolicyRequest request);

    BusinessPolicyResponse toggleActive(String id);

    void delete(String id);
}
