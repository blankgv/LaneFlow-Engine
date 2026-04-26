package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.admin.model.User;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.workflow.model.WorkflowCollaborator;
import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;
import com.laneflow.engine.modules.workflow.repository.WorkflowCollaboratorRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import com.laneflow.engine.modules.workflow.request.WorkflowCollaborationPresenceRequest;
import com.laneflow.engine.modules.workflow.request.WorkflowDraftSyncRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowCollaborationPresenceResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowDraftSyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowRealtimeCollaborationServiceImpl implements WorkflowRealtimeCollaborationService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowCollaboratorRepository workflowCollaboratorRepository;
    private final UserRepository userRepository;
    private final WorkflowService workflowService;
    private final SimpMessagingTemplate messagingTemplate;

    private final ConcurrentMap<String, Set<String>> activeUsersByWorkflow = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SessionPresence> sessionPresenceBySessionId = new ConcurrentHashMap<>();

    @Override
    public WorkflowCollaborationPresenceResponse join(String workflowId,
                                                     String sessionId,
                                                     WorkflowCollaborationPresenceRequest request) {
        WorkflowDefinition workflow = requireEditableAccess(workflowId, request.username());
        activeUsersByWorkflow.computeIfAbsent(workflowId, ignored -> ConcurrentHashMap.newKeySet())
                .add(request.username());
        sessionPresenceBySessionId.put(sessionId, new SessionPresence(workflowId, request.username()));

        WorkflowCollaborationPresenceResponse response = buildPresenceResponse(
                workflowId,
                "JOINED",
                request.username()
        );
        messagingTemplate.convertAndSend("/topic/workflows/" + workflowId + "/presence", response);
        log.info("Usuario {} se unio a la colaboracion en workflow {}", request.username(), workflow.getCode());
        return response;
    }

    @Override
    public WorkflowCollaborationPresenceResponse leave(String workflowId,
                                                      String sessionId,
                                                      WorkflowCollaborationPresenceRequest request) {
        requireWorkflowAccess(workflowId, request.username());
        removePresence(workflowId, request.username(), sessionId);
        WorkflowCollaborationPresenceResponse response = buildPresenceResponse(
                workflowId,
                "LEFT",
                request.username()
        );
        messagingTemplate.convertAndSend("/topic/workflows/" + workflowId + "/presence", response);
        return response;
    }

    @Override
    public WorkflowDraftSyncResponse saveDraft(String workflowId, WorkflowDraftSyncRequest request) {
        requireEditableAccess(workflowId, request.username());

        WorkflowResponse updated = workflowService.update(
                workflowId,
                new com.laneflow.engine.modules.workflow.request.UpdateWorkflowRequest(
                        null,
                        null,
                        request.bpmnXml(),
                        null,
                        null,
                        null
                ),
                request.username()
        );

        WorkflowDraftSyncResponse response = new WorkflowDraftSyncResponse(
                workflowId,
                "DRAFT_SAVED",
                updated.draftBpmnXml(),
                updated.lastModifiedBy(),
                updated.updatedAt()
        );
        messagingTemplate.convertAndSend("/topic/workflows/" + workflowId + "/draft", response);
        return response;
    }

    @Override
    public void handleDisconnect(String sessionId) {
        SessionPresence presence = sessionPresenceBySessionId.remove(sessionId);
        if (presence == null) {
            return;
        }

        removePresence(presence.workflowId(), presence.username(), null);
        WorkflowCollaborationPresenceResponse response = buildPresenceResponse(
                presence.workflowId(),
                "DISCONNECTED",
                presence.username()
        );
        messagingTemplate.convertAndSend("/topic/workflows/" + presence.workflowId() + "/presence", response);
    }

    private WorkflowDefinition requireEditableAccess(String workflowId, String username) {
        WorkflowDefinition workflow = requireWorkflowAccess(workflowId, username);
        if (workflow.getStatus() != WorkflowStatus.DRAFT) {
            throw new IllegalStateException("La colaboracion en tiempo real solo esta disponible para borradores.");
        }
        return workflow;
    }

    private WorkflowDefinition requireWorkflowAccess(String workflowId, String username) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + workflowId));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        boolean isOwner = username.equals(workflow.getCreatedBy());
        boolean isCollaborator = workflowCollaboratorRepository
                .findByWorkflowDefinitionIdAndUserId(workflowId, user.getId())
                .map(WorkflowCollaborator::getId)
                .isPresent();

        if (!isOwner && !isCollaborator) {
            throw new IllegalStateException("El usuario no tiene acceso colaborativo a esta politica.");
        }

        return workflow;
    }

    private void removePresence(String workflowId, String username, String sessionId) {
        Set<String> activeUsers = activeUsersByWorkflow.get(workflowId);
        if (activeUsers != null) {
            activeUsers.remove(username);
            if (activeUsers.isEmpty()) {
                activeUsersByWorkflow.remove(workflowId);
            }
        }

        if (sessionId != null) {
            sessionPresenceBySessionId.remove(sessionId);
        }
    }

    private WorkflowCollaborationPresenceResponse buildPresenceResponse(String workflowId,
                                                                       String action,
                                                                       String username) {
        Set<String> activeUsers = activeUsersByWorkflow.getOrDefault(workflowId, Set.of());
        List<String> users = new ArrayList<>(activeUsers);
        users.sort(String::compareToIgnoreCase);
        return new WorkflowCollaborationPresenceResponse(
                workflowId,
                action,
                username,
                users.size(),
                users,
                LocalDateTime.now()
        );
    }

    private record SessionPresence(String workflowId, String username) {
    }
}
