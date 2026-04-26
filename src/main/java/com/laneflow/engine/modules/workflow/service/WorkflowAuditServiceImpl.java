package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.WorkflowAudit;
import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowAuditAction;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;
import com.laneflow.engine.modules.workflow.repository.WorkflowAuditRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import com.laneflow.engine.modules.workflow.response.WorkflowHistoryItemResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorkflowAuditServiceImpl implements WorkflowAuditService {

    private final WorkflowAuditRepository workflowAuditRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowAccessService workflowAccessService;

    @Override
    public void record(WorkflowDefinition workflow,
                       WorkflowAuditAction action,
                       String description,
                       String username,
                       WorkflowStatus statusBefore,
                       WorkflowStatus statusAfter,
                       Map<String, Object> metadata) {
        workflowAuditRepository.save(WorkflowAudit.builder()
                .workflowDefinitionId(workflow.getId())
                .workflowCode(workflow.getCode())
                .workflowName(workflow.getName())
                .action(action)
                .description(description)
                .username(username)
                .statusBefore(statusBefore)
                .statusAfter(statusAfter)
                .metadata(metadata == null ? Map.of() : metadata)
                .build());
    }

    @Override
    public WorkflowHistoryResponse getHistory(String workflowId, String username) {
        workflowAccessService.requireReadable(workflowId, username);
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId).orElse(null);
        List<WorkflowAudit> audits = workflowAuditRepository.findByWorkflowDefinitionIdOrderByCreatedAtAsc(workflowId);
        List<WorkflowHistoryItemResponse> history = audits.stream()
                .map(this::toHistoryItem)
                .toList();

        WorkflowAudit lastAudit = audits.isEmpty() ? null : audits.get(audits.size() - 1);

        return new WorkflowHistoryResponse(
                workflowId,
                workflow != null ? workflow.getCode() : lastAudit != null ? lastAudit.getWorkflowCode() : null,
                workflow != null ? workflow.getName() : lastAudit != null ? lastAudit.getWorkflowName() : null,
                workflow != null ? workflow.getStatus() : null,
                workflow != null ? workflow.getCurrentVersion() : null,
                history
        );
    }

    private WorkflowHistoryItemResponse toHistoryItem(WorkflowAudit audit) {
        return new WorkflowHistoryItemResponse(
                audit.getId(),
                audit.getAction(),
                resolveTitle(audit),
                resolveMessage(audit),
                audit.getUsername(),
                audit.getStatusBefore(),
                audit.getStatusAfter(),
                audit.getMetadata() == null ? Map.of() : audit.getMetadata(),
                audit.getCreatedAt()
        );
    }

    private String resolveTitle(WorkflowAudit audit) {
        return switch (audit.getAction()) {
            case WORKFLOW_CREATED -> "Politica creada";
            case WORKFLOW_UPDATED -> "Borrador actualizado";
            case WORKFLOW_PUBLISHED -> "Politica publicada";
            case WORKFLOW_DELETED -> "Politica eliminada";
            case VERSION_DRAFT_CREATED -> "Version borrador creada";
            case VERSION_PUBLISHED -> "Version publicada";
            case COLLABORATOR_INVITED -> "Colaborador invitado";
            case INVITATION_ACCEPTED -> "Invitacion aceptada";
            case INVITATION_REJECTED -> "Invitacion rechazada";
        };
    }

    private String resolveMessage(WorkflowAudit audit) {
        String actor = audit.getUsername() == null || audit.getUsername().isBlank()
                ? "el sistema"
                : audit.getUsername();

        return switch (audit.getAction()) {
            case WORKFLOW_CREATED ->
                    "La politica fue creada por %s.".formatted(actor);
            case WORKFLOW_UPDATED ->
                    "El borrador de la politica fue actualizado por %s.".formatted(actor);
            case WORKFLOW_PUBLISHED ->
                    "La politica fue publicada por %s.".formatted(actor);
            case WORKFLOW_DELETED ->
                    "La politica fue eliminada por %s.".formatted(actor);
            case VERSION_DRAFT_CREATED ->
                    "Se creo una nueva version borrador por %s.".formatted(actor);
            case VERSION_PUBLISHED ->
                    "Se publico una version de la politica por %s.".formatted(actor);
            case COLLABORATOR_INVITED ->
                    "Se invito a %s a colaborar en la politica.".formatted(resolveMetadataValue(audit, "invitedUsername", "un usuario"));
            case INVITATION_ACCEPTED ->
                    "La invitacion fue aceptada por %s.".formatted(actor);
            case INVITATION_REJECTED ->
                    "La invitacion fue rechazada por %s.".formatted(actor);
        };
    }

    private String resolveMetadataValue(WorkflowAudit audit, String key, String fallback) {
        if (audit.getMetadata() == null) {
            return fallback;
        }

        Object value = audit.getMetadata().get(key);
        if (value == null) {
            return fallback;
        }

        String text = value.toString().trim();
        return text.isEmpty() ? fallback : text;
    }
}
