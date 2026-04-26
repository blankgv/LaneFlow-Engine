package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.request.CreateWorkflowRequest;
import com.laneflow.engine.modules.workflow.request.UpdateWorkflowRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowSummaryResponse;

import java.util.List;

public interface WorkflowService {

    List<WorkflowSummaryResponse> findAll(String username);

    WorkflowResponse findById(String id, String username);

    WorkflowResponse create(CreateWorkflowRequest request, String createdBy);

    WorkflowResponse update(String id, UpdateWorkflowRequest request, String updatedBy);

    WorkflowResponse publish(String id, String publishedBy);

    void delete(String id, String deletedBy);

    WorkflowResponse validate(String id, String username);
}
