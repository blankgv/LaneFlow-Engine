package com.laneflow.engine.modules.workflow.listener;

import com.laneflow.engine.modules.workflow.service.WorkflowRealtimeCollaborationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WorkflowCollaborationSessionListener {

    private final WorkflowRealtimeCollaborationService workflowRealtimeCollaborationService;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        if (sessionId != null) {
            workflowRealtimeCollaborationService.handleDisconnect(sessionId);
        }
    }
}
