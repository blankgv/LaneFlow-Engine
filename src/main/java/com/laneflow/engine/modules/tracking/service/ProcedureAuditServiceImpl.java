package com.laneflow.engine.modules.tracking.service;

import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import com.laneflow.engine.modules.operation.repository.ProcedureRepository;
import com.laneflow.engine.modules.tracking.model.ProcedureAudit;
import com.laneflow.engine.modules.tracking.model.enums.ProcedureAuditAction;
import com.laneflow.engine.modules.tracking.repository.ProcedureAuditRepository;
import com.laneflow.engine.modules.tracking.response.ProcedureHistoryItemResponse;
import com.laneflow.engine.modules.tracking.response.ProcedureHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProcedureAuditServiceImpl implements ProcedureAuditService {

    private final ProcedureAuditRepository repository;
    private final ProcedureRepository procedureRepository;

    @Override
    public void record(Procedure procedure,
                       ProcedureAuditAction action,
                       String description,
                       String username,
                       String taskId,
                       String nodeId,
                       String nodeName,
                       ProcedureStatus statusBefore,
                       ProcedureStatus statusAfter,
                       Map<String, Object> metadata) {
        repository.save(ProcedureAudit.builder()
                .procedureId(procedure.getId())
                .procedureCode(procedure.getCode())
                .action(action)
                .description(description)
                .username(username)
                .taskId(taskId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .statusBefore(statusBefore)
                .statusAfter(statusAfter)
                .metadata(metadata == null ? Map.of() : metadata)
                .build());
    }

    @Override
    public ProcedureHistoryResponse getHistory(String procedureId) {
        Procedure procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new IllegalArgumentException("Tramite no encontrado: " + procedureId));

        List<ProcedureHistoryItemResponse> history = repository.findByProcedureIdOrderByCreatedAtAsc(procedureId)
                .stream()
                .map(this::toHistoryItem)
                .toList();

        return new ProcedureHistoryResponse(
                procedure.getId(),
                procedure.getCode(),
                procedure.getWorkflowName(),
                procedure.getApplicantName(),
                procedure.getStatus(),
                procedure.getStartedAt(),
                procedure.getCompletedAt(),
                history
        );
    }

    private ProcedureHistoryItemResponse toHistoryItem(ProcedureAudit audit) {
        return new ProcedureHistoryItemResponse(
                audit.getId(),
                audit.getAction(),
                resolveTitle(audit),
                resolveMessage(audit),
                audit.getUsername(),
                audit.getTaskId(),
                audit.getNodeId(),
                audit.getNodeName(),
                audit.getStatusBefore(),
                audit.getStatusAfter(),
                sanitizeMetadata(audit),
                audit.getCreatedAt()
        );
    }

    private String resolveTitle(ProcedureAudit audit) {
        if (isProcedureCompletionAudit(audit)) {
            return "Tramite finalizado";
        }
        if (isFormAttachmentAudit(audit)) {
            return switch (audit.getAction()) {
                case EVIDENCE_UPLOADED -> "Adjunto de formulario cargado";
                case EVIDENCE_DELETED -> "Adjunto de formulario eliminado";
                default -> "";
            };
        }
        return switch (audit.getAction()) {
            case PROCEDURE_STARTED -> "Tramite iniciado";
            case TASK_CLAIMED -> "Tarea tomada";
            case TASK_COMPLETED -> "Actividad completada";
            case TASK_APPROVED -> "Tramite aprobado";
            case TASK_OBSERVED -> "Tramite observado";
            case TASK_REJECTED -> "Tramite rechazado";
            case OBSERVATION_RESOLVED -> "Observacion subsanada";
            case EVIDENCE_UPLOADED -> "Evidencia adjuntada";
            case EVIDENCE_DELETED -> "Evidencia eliminada";
        };
    }

    private String resolveMessage(ProcedureAudit audit) {
        String actor = audit.getUsername() == null || audit.getUsername().isBlank()
                ? "el sistema"
                : audit.getUsername();

        if (isProcedureCompletionAudit(audit)) {
            return "El tramite fue finalizado por %s en la actividad %s.".formatted(actor, resolveNodeLabel(audit));
        }
        if (isFormAttachmentAudit(audit)) {
            return switch (audit.getAction()) {
                case EVIDENCE_UPLOADED ->
                        "Se adjunto el archivo %s en el formulario de la actividad %s por %s."
                                .formatted(resolveMetadataValue(audit, "fileName", "sin nombre"), resolveNodeLabel(audit), actor);
                case EVIDENCE_DELETED ->
                        "Se elimino el archivo %s del formulario de la actividad %s."
                                .formatted(resolveMetadataValue(audit, "fileName", "sin nombre"), resolveNodeLabel(audit));
                default -> "";
            };
        }

        return switch (audit.getAction()) {
            case PROCEDURE_STARTED ->
                    "El tramite fue iniciado por %s y enviado al flujo de trabajo.".formatted(actor);
            case TASK_CLAIMED ->
                    "La tarea %s fue tomada por %s.".formatted(resolveNodeLabel(audit), actor);
            case TASK_COMPLETED ->
                    "La actividad %s fue completada por %s.".formatted(resolveNodeLabel(audit), actor);
            case TASK_APPROVED ->
                    "El tramite fue aprobado en la actividad %s por %s.".formatted(resolveNodeLabel(audit), actor);
            case TASK_OBSERVED ->
                    "El tramite fue observado en la actividad %s por %s.".formatted(resolveNodeLabel(audit), actor);
            case TASK_REJECTED ->
                    "El tramite fue rechazado en la actividad %s por %s.".formatted(resolveNodeLabel(audit), actor);
            case OBSERVATION_RESOLVED ->
                    "La observacion fue subsanada por %s y el tramite volvio al flujo.".formatted(actor);
            case EVIDENCE_UPLOADED ->
                    "Se adjunto la evidencia %s por %s.".formatted(resolveMetadataValue(audit, "fileName", "sin nombre"), actor);
            case EVIDENCE_DELETED ->
                    "Se elimino la evidencia %s.".formatted(resolveMetadataValue(audit, "fileName", "sin nombre"));
        };
    }

    private String resolveNodeLabel(ProcedureAudit audit) {
        if (audit.getNodeName() != null && !audit.getNodeName().isBlank()) {
            return audit.getNodeName();
        }
        if (audit.getNodeId() != null && !audit.getNodeId().isBlank()) {
            return audit.getNodeId();
        }
        return "actual";
    }

    private String resolveMetadataValue(ProcedureAudit audit, String key, String fallback) {
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

    private Map<String, Object> sanitizeMetadata(ProcedureAudit audit) {
        Map<String, Object> metadata = audit.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }

        return switch (audit.getAction()) {
            case TASK_COMPLETED, TASK_APPROVED, TASK_OBSERVED, TASK_REJECTED -> {
                String comment = resolveMetadataValue(audit, "comment", "");
                yield comment.isBlank() ? Map.of() : Map.of("comment", comment);
            }
            case OBSERVATION_RESOLVED -> Map.of(
                    "resubmissionCount", metadata.getOrDefault("resubmissionCount", 0)
            );
            case EVIDENCE_UPLOADED, EVIDENCE_DELETED -> {
                String fileName = resolveMetadataValue(audit, "fileName", "");
                String fieldName = resolveMetadataValue(audit, "fieldName", "");
                String scope = isFormAttachmentAudit(audit) ? "FORM_FIELD" : "GLOBAL";
                if (fileName.isBlank() && fieldName.isBlank()) {
                    yield Map.of("scope", scope);
                }
                if (fieldName.isBlank() && fileName.isBlank()) {
                    yield Map.of();
                }
                if (fieldName.isBlank()) {
                    yield Map.of(
                            "scope", scope,
                            "fileName", fileName
                    );
                }
                if (fileName.isBlank()) {
                    yield Map.of(
                            "scope", scope,
                            "fieldName", fieldName
                    );
                }
                yield Map.of(
                        "scope", scope,
                        "fileName", fileName,
                        "fieldName", fieldName
                );
            }
            default -> Map.of();
        };
    }

    private boolean isProcedureCompletionAudit(ProcedureAudit audit) {
        return (audit.getAction() == ProcedureAuditAction.TASK_COMPLETED
                || audit.getAction() == ProcedureAuditAction.TASK_APPROVED)
                && audit.getStatusAfter() == ProcedureStatus.COMPLETED;
    }

    private boolean isFormAttachmentAudit(ProcedureAudit audit) {
        String fieldName = resolveMetadataValue(audit, "fieldName", "");
        return (audit.getAction() == ProcedureAuditAction.EVIDENCE_UPLOADED
                || audit.getAction() == ProcedureAuditAction.EVIDENCE_DELETED)
                && (!fieldName.isBlank()
                || (audit.getNodeId() != null && !audit.getNodeId().isBlank()));
    }
}
