package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.request.CreateVersionRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowVersionResponse;

import java.util.List;

public interface WorkflowVersionService {

    List<WorkflowVersionResponse> findByWorkflow(String workflowId);

    WorkflowVersionResponse findByWorkflowAndVersion(String workflowId, int versionNumber);

    WorkflowVersionResponse createDraft(String workflowId, CreateVersionRequest request, String createdBy);

    WorkflowVersionResponse publish(String workflowId, int versionNumber, String publishedBy);
}
