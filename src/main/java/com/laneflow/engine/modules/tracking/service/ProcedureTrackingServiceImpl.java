package com.laneflow.engine.modules.tracking.service;

import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import com.laneflow.engine.modules.operation.repository.ProcedureRepository;
import com.laneflow.engine.modules.tracking.model.ProcedureAudit;
import com.laneflow.engine.modules.tracking.model.enums.ProcedureAuditAction;
import com.laneflow.engine.modules.tracking.repository.ProcedureAuditRepository;
import com.laneflow.engine.modules.tracking.response.ProcedureStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProcedureTrackingServiceImpl implements ProcedureTrackingService {

    private final ProcedureRepository procedureRepository;
    private final ProcedureAuditRepository procedureAuditRepository;

    @Override
    public ProcedureStatusResponse getStatus(String procedureId) {
        Procedure procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new IllegalArgumentException("Tramite no encontrado: " + procedureId));

        ProcedureAudit latestAudit = procedureAuditRepository.findTopByProcedureIdOrderByCreatedAtDesc(procedureId)
                .orElse(null);

        return new ProcedureStatusResponse(
                procedure.getId(),
                procedure.getCode(),
                procedure.getWorkflowName(),
                procedure.getApplicantName(),
                procedure.getStatus(),
                resolveCurrentStage(procedure),
                resolveStatusMessage(procedure),
                procedure.getCurrentAssigneeUsername(),
                latestAudit != null ? resolveTitle(latestAudit) : null,
                latestAudit != null ? resolveMessage(latestAudit) : null,
                procedure.getStartedAt(),
                procedure.getUpdatedAt(),
                procedure.getCompletedAt()
        );
    }

    private String resolveCurrentStage(Procedure procedure) {
        if (procedure.getCurrentNodeName() != null && !procedure.getCurrentNodeName().isBlank()) {
            return procedure.getCurrentNodeName();
        }
        return switch (procedure.getStatus()) {
            case STARTED -> "Inicio";
            case IN_PROGRESS -> "En curso";
            case OBSERVED -> "Observado";
            case REJECTED -> "Rechazado";
            case COMPLETED -> "Finalizado";
            case CANCELLED -> "Cancelado";
        };
    }

    private String resolveStatusMessage(Procedure procedure) {
        return switch (procedure.getStatus()) {
            case STARTED -> "El tramite fue registrado y esta comenzando su flujo.";
            case IN_PROGRESS -> "El tramite se encuentra en revision dentro del flujo.";
            case OBSERVED -> "El tramite tiene observaciones y requiere subsanacion.";
            case REJECTED -> "El tramite fue rechazado.";
            case COMPLETED -> "El tramite finalizo correctamente.";
            case CANCELLED -> "El tramite fue cancelado.";
        };
    }

    private String resolveTitle(ProcedureAudit audit) {
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
                    "Se adjunto una nueva evidencia al tramite por %s.".formatted(actor);
            case EVIDENCE_DELETED ->
                    "Se elimino una evidencia del tramite.";
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
}
