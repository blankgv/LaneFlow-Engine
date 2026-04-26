package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;

import java.util.List;

public interface WorkflowAccessService {

    WorkflowDefinition requireReadable(String workflowId, String username);

    WorkflowDefinition requireWritable(String workflowId, String username);

    void requireReadable(WorkflowDefinition workflow, String username);

    void requireWritable(WorkflowDefinition workflow, String username);

    boolean canRead(WorkflowDefinition workflow, String username);

    boolean canWrite(WorkflowDefinition workflow, String username);

    List<WorkflowDefinition> filterReadable(List<WorkflowDefinition> workflows, String username);
}
