package com.laneflow.engine.modules.workflow.response;

public record WorkflowEditorTaskResponse(
        String nodeId,
        String nodeName,
        String swimlaneId,
        String departmentId,
        String departmentCode,
        String departmentName,
        String requiredAction,
        String formId,
        String formTitle
) {}
