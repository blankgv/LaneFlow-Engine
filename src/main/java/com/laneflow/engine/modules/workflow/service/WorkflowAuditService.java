package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowAuditAction;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;
import com.laneflow.engine.modules.workflow.response.WorkflowHistoryResponse;

import java.util.Map;

public interface WorkflowAuditService {

    void record(WorkflowDefinition workflow,
                WorkflowAuditAction action,
                String description,
                String username,
                WorkflowStatus statusBefore,
                WorkflowStatus statusAfter,
                Map<String, Object> metadata);

    WorkflowHistoryResponse getHistory(String workflowId, String username);
}
