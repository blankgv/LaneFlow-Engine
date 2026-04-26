package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.response.WorkflowEditorResponse;

public interface WorkflowEditorService {

    WorkflowEditorResponse getEditorSnapshot(String workflowId, String username);
}
