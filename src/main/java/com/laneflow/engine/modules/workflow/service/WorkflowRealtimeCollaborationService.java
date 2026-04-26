package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.request.WorkflowCollaborationPresenceRequest;
import com.laneflow.engine.modules.workflow.request.WorkflowDraftSyncRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowCollaborationPresenceResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowDraftSyncResponse;

public interface WorkflowRealtimeCollaborationService {

    WorkflowCollaborationPresenceResponse join(String workflowId, String sessionId, String username, WorkflowCollaborationPresenceRequest request);

    WorkflowCollaborationPresenceResponse leave(String workflowId, String sessionId, String username, WorkflowCollaborationPresenceRequest request);

    WorkflowDraftSyncResponse saveDraft(String workflowId, String username, WorkflowDraftSyncRequest request);

    void handleDisconnect(String sessionId);
}
