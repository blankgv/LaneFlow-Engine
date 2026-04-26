package com.laneflow.engine.modules.workflow.response;

import java.util.List;

public record WorkflowEditorResponse(
        WorkflowResponse workflow,
        boolean canEdit,
        List<WorkflowEditorTaskResponse> tasks,
        List<DynamicFormResponse> forms,
        List<WorkflowCollaboratorResponse> collaborators,
        List<WorkflowInvitationResponse> invitations
) {}
