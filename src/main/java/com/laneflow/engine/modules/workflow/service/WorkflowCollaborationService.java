package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.request.CreateWorkflowInvitationRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowCollaboratorResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowInviteeResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowInvitationResponse;

import java.util.List;

public interface WorkflowCollaborationService {

    List<WorkflowCollaboratorResponse> findCollaborators(String workflowId, String username);

    List<WorkflowInviteeResponse> findEligibleInvitees(String workflowId, String currentUsername);

    List<WorkflowInvitationResponse> findInvitationsByWorkflow(String workflowId, String username);

    List<WorkflowInvitationResponse> findMyInvitations(String username);

    WorkflowInvitationResponse invite(String workflowId, CreateWorkflowInvitationRequest request, String invitedByUsername);

    WorkflowInvitationResponse accept(String invitationId, String username);

    WorkflowInvitationResponse reject(String invitationId, String username);
}
