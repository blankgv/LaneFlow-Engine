package com.laneflow.engine.modules.workflow.controller;

import com.laneflow.engine.modules.workflow.request.WorkflowCollaborationPresenceRequest;
import com.laneflow.engine.modules.workflow.request.WorkflowDraftSyncRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowCollaborationPresenceResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowDraftSyncResponse;
import com.laneflow.engine.modules.workflow.service.WorkflowRealtimeCollaborationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WorkflowRealtimeCollaborationController {

    private final WorkflowRealtimeCollaborationService workflowRealtimeCollaborationService;

    @MessageMapping("/workflows/{workflowId}/presence.join")
    public WorkflowCollaborationPresenceResponse join(@DestinationVariable String workflowId,
                                                      @Header("simpSessionId") String sessionId,
                                                      Principal principal,
                                                      @Payload WorkflowCollaborationPresenceRequest request) {
        return workflowRealtimeCollaborationService.join(workflowId, sessionId, requireUsername(principal), request);
    }

    @MessageMapping("/workflows/{workflowId}/presence.leave")
    public WorkflowCollaborationPresenceResponse leave(@DestinationVariable String workflowId,
                                                       @Header("simpSessionId") String sessionId,
                                                       Principal principal,
                                                       @Payload WorkflowCollaborationPresenceRequest request) {
        return workflowRealtimeCollaborationService.leave(workflowId, sessionId, requireUsername(principal), request);
    }

    @MessageMapping("/workflows/{workflowId}/draft.save")
    public WorkflowDraftSyncResponse saveDraft(@DestinationVariable String workflowId,
                                               Principal principal,
                                               @Payload WorkflowDraftSyncRequest request) {
        return workflowRealtimeCollaborationService.saveDraft(workflowId, requireUsername(principal), request);
    }

    private String requireUsername(Principal principal) {
        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new IllegalStateException("La sesion WebSocket no esta autenticada.");
        }
        return principal.getName();
    }
}
