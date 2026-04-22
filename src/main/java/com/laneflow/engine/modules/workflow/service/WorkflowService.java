package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.request.CreateWorkflowRequest;
import com.laneflow.engine.modules.workflow.request.UpdateWorkflowRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowSummaryResponse;

import java.util.List;

public interface WorkflowService {

    List<WorkflowSummaryResponse> findAll();

    WorkflowResponse findById(String id);

    WorkflowResponse create(CreateWorkflowRequest request, String createdBy);

    WorkflowResponse update(String id, UpdateWorkflowRequest request);

    WorkflowResponse publish(String id, String publishedBy);

    void delete(String id);

    WorkflowResponse validate(String id);
}
