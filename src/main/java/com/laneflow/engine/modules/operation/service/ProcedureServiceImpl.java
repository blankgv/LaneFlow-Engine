package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.operation.model.Applicant;
import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.model.enums.ApplicantType;
import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import com.laneflow.engine.modules.operation.model.enums.TaskAction;
import com.laneflow.engine.modules.operation.repository.ApplicantRepository;
import com.laneflow.engine.modules.operation.repository.ProcedureRepository;
import com.laneflow.engine.modules.operation.request.ResolveObservationRequest;
import com.laneflow.engine.modules.operation.request.StartProcedureRequest;
import com.laneflow.engine.modules.operation.response.ProcedureResponse;
import com.laneflow.engine.modules.tracking.model.enums.ProcedureAuditAction;
import com.laneflow.engine.modules.tracking.model.enums.NotificationType;
import com.laneflow.engine.modules.tracking.service.ProcedureAuditService;
import com.laneflow.engine.modules.tracking.service.ProcedureNotificationService;
import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcedureServiceImpl implements ProcedureService {

    private static final DateTimeFormatter CODE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final int ACTIVE_TASK_LOOKUP_ATTEMPTS = 10;
    private static final long ACTIVE_TASK_LOOKUP_DELAY_MS = 300L;

    private final ProcedureRepository procedureRepository;
    private final ApplicantRepository applicantRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final RuntimeService runtimeService;
    private final org.camunda.bpm.engine.TaskService taskService;
    private final ProcedureAuditService procedureAuditService;
    private final ProcedureNotificationService procedureNotificationService;
    private final TaskFormSubmissionValidator taskFormSubmissionValidator;

    @Override
    public ProcedureResponse start(StartProcedureRequest request, String startedBy) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(request.workflowDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + request.workflowDefinitionId()));

        if (workflow.getStatus() != WorkflowStatus.PUBLISHED) {
            throw new IllegalStateException("Solo se pueden iniciar tramites con workflows publicados.");
        }

        if (workflow.getCamundaProcessKey() == null || workflow.getCamundaProcessKey().isBlank()) {
            throw new IllegalStateException("El workflow no tiene process key de Camunda configurado.");
        }

        Applicant applicant = applicantRepository.findById(request.applicantId())
                .orElseThrow(() -> new IllegalArgumentException("Solicitante no encontrado: " + request.applicantId()));

        if (!applicant.isActive()) {
            throw new IllegalStateException("No se puede iniciar un tramite para un solicitante inactivo.");
        }

        LocalDateTime now = LocalDateTime.now();
        ProcedureStatus statusBefore = ProcedureStatus.STARTED;
        Procedure procedure = procedureRepository.save(Procedure.builder()
                .code(generateCode())
                .workflowDefinitionId(workflow.getId())
                .workflowCode(workflow.getCode())
                .workflowName(workflow.getName())
                .workflowVersion(workflow.getCurrentVersion())
                .camundaProcessKey(workflow.getCamundaProcessKey())
                .applicantId(applicant.getId())
                .applicantDocumentNumber(applicant.getDocumentNumber())
                .applicantName(resolveApplicantName(applicant))
                .status(ProcedureStatus.STARTED)
                .formData(request.formData() != null ? request.formData() : Map.of())
                .startedBy(startedBy)
                .startedAt(now)
                .build());
        String startedInstanceId = null;

        try {
            Map<String, Object> variables = buildProcessVariables(procedure);
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                    workflow.getCamundaProcessKey(),
                    procedure.getId(),
                    variables
            );
            startedInstanceId = instance.getId();

            Task activeTask = waitForFirstActiveTask(instance.getId());

            procedure.setCamundaProcessInstanceId(instance.getId());
            procedure.setStatus(ProcedureStatus.IN_PROGRESS);
            if (activeTask != null) {
                procedure.setCurrentTaskId(activeTask.getId());
                procedure.setCurrentNodeId(activeTask.getTaskDefinitionKey());
                procedure.setCurrentNodeName(activeTask.getName());
                taskFormSubmissionValidator.validate(
                        procedure,
                        activeTask.getId(),
                        activeTask.getTaskDefinitionKey(),
                        request.formData(),
                        procedure.getFormData()
                );
            }
            procedure.setUpdatedAt(LocalDateTime.now());
            Procedure saved = procedureRepository.save(procedure);
            procedureAuditService.record(
                    saved,
                    ProcedureAuditAction.PROCEDURE_STARTED,
                    "Tramite iniciado y enviado a Camunda.",
                    startedBy,
                    saved.getCurrentTaskId(),
                    saved.getCurrentNodeId(),
                    saved.getCurrentNodeName(),
                    statusBefore,
                    saved.getStatus(),
                    Map.of(
                            "workflowDefinitionId", saved.getWorkflowDefinitionId(),
                            "applicantId", saved.getApplicantId(),
                            "camundaProcessInstanceId", saved.getCamundaProcessInstanceId()
                    )
            );
            procedureNotificationService.notifyApplicant(saved, NotificationType.PROCEDURE_STARTED);
            return toResponse(saved);
        } catch (Exception e) {
            if (startedInstanceId != null) {
                safeDeleteProcessInstance(startedInstanceId, "Rollback por validacion al iniciar tramite");
            }
            procedureRepository.delete(procedure);
            throw new IllegalStateException("Error al iniciar el tramite en Camunda: " + e.getMessage(), e);
        }
    }

    @Override
    public ProcedureResponse resolveObservation(String id, ResolveObservationRequest request, String resolvedBy) {
        Procedure procedure = procedureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tramite no encontrado: " + id));
        ProcedureStatus statusBefore = procedure.getStatus();
        String originalCamundaProcessInstanceId = procedure.getCamundaProcessInstanceId();
        String originalPreviousCamundaProcessInstanceId = procedure.getPreviousCamundaProcessInstanceId();
        Map<String, Object> originalFormData = procedure.getFormData() == null
                ? null
                : new HashMap<>(procedure.getFormData());
        String originalLastAction = procedure.getLastAction();
        String originalLastComment = procedure.getLastComment();
        int originalResubmissionCount = procedure.getResubmissionCount();
        String originalResolvedObservationBy = procedure.getResolvedObservationBy();
        LocalDateTime originalResolvedObservationAt = procedure.getResolvedObservationAt();
        String originalResolvedObservationComment = procedure.getResolvedObservationComment();
        LocalDateTime originalCompletedAt = procedure.getCompletedAt();
        LocalDateTime originalUpdatedAt = procedure.getUpdatedAt();
        String originalCurrentTaskId = procedure.getCurrentTaskId();
        String originalCurrentNodeId = procedure.getCurrentNodeId();
        String originalCurrentNodeName = procedure.getCurrentNodeName();
        String originalCurrentAssigneeUsername = procedure.getCurrentAssigneeUsername();
        LocalDateTime originalClaimedAt = procedure.getClaimedAt();

        if (procedure.getStatus() != ProcedureStatus.OBSERVED) {
            throw new IllegalStateException("Solo se pueden subsanar tramites en estado OBSERVED.");
        }

        WorkflowDefinition workflow = workflowDefinitionRepository.findById(procedure.getWorkflowDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + procedure.getWorkflowDefinitionId()));

        if (workflow.getStatus() != WorkflowStatus.PUBLISHED) {
            throw new IllegalStateException("Solo se pueden subsanar tramites con workflows publicados.");
        }

        if (workflow.getCamundaProcessKey() == null || workflow.getCamundaProcessKey().isBlank()) {
            throw new IllegalStateException("El workflow no tiene process key de Camunda configurado.");
        }

        Map<String, Object> mergedFormData = new HashMap<>();
        if (procedure.getFormData() != null) {
            mergedFormData.putAll(procedure.getFormData());
        }
        if (request.formData() != null) {
            mergedFormData.putAll(request.formData());
        }

        String previousInstanceId = procedure.getCamundaProcessInstanceId();
        procedure.setPreviousCamundaProcessInstanceId(previousInstanceId);
        procedure.setFormData(mergedFormData);
        procedure.setLastAction(TaskAction.RESOLVE_OBSERVATION.name());
        procedure.setLastComment(trimToNull(request.comment()));
        procedure.setResolvedObservationBy(resolvedBy);
        procedure.setResolvedObservationAt(LocalDateTime.now());
        procedure.setResolvedObservationComment(trimToNull(request.comment()));
        procedure.setResubmissionCount(procedure.getResubmissionCount() + 1);
        procedure.setCompletedAt(null);
        procedure.setUpdatedAt(LocalDateTime.now());
        String restartedInstanceId = null;

        try {
            Map<String, Object> variables = buildProcessVariables(procedure);
            variables.put("previousCamundaProcessInstanceId", previousInstanceId);
            variables.put("resubmissionCount", procedure.getResubmissionCount());
            variables.put("resolvedObservationBy", resolvedBy);
            variables.put("resolvedObservationComment", request.comment());

            ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                    workflow.getCamundaProcessKey(),
                    procedure.getId(),
                    variables
            );
            restartedInstanceId = instance.getId();

            Task activeTask = waitForFirstActiveTask(instance.getId());

            procedure.setCamundaProcessInstanceId(instance.getId());
            procedure.setStatus(ProcedureStatus.IN_PROGRESS);
            procedure.setCurrentAssigneeUsername(null);
            procedure.setClaimedAt(null);

            if (activeTask != null) {
                procedure.setCurrentTaskId(activeTask.getId());
                procedure.setCurrentNodeId(activeTask.getTaskDefinitionKey());
                procedure.setCurrentNodeName(activeTask.getName());
                taskFormSubmissionValidator.validate(
                        procedure,
                        activeTask.getId(),
                        activeTask.getTaskDefinitionKey(),
                        request.formData(),
                        mergedFormData
                );
            } else {
                procedure.setCurrentTaskId(null);
                procedure.setCurrentNodeId(null);
                procedure.setCurrentNodeName(null);
            }

            procedure.setUpdatedAt(LocalDateTime.now());
            Procedure saved = procedureRepository.save(procedure);
            procedureAuditService.record(
                    saved,
                    ProcedureAuditAction.OBSERVATION_RESOLVED,
                    "Observacion subsanada y tramite reiniciado en Camunda.",
                    resolvedBy,
                    saved.getCurrentTaskId(),
                    saved.getCurrentNodeId(),
                    saved.getCurrentNodeName(),
                    statusBefore,
                    saved.getStatus(),
                    Map.of(
                            "previousCamundaProcessInstanceId", previousInstanceId == null ? "" : previousInstanceId,
                            "camundaProcessInstanceId", saved.getCamundaProcessInstanceId(),
                            "resubmissionCount", saved.getResubmissionCount()
                    )
            );
            procedureNotificationService.notifyApplicant(saved, NotificationType.OBSERVATION_RESOLVED);
            return toResponse(saved);
        } catch (Exception e) {
            procedure.setCamundaProcessInstanceId(originalCamundaProcessInstanceId);
            procedure.setPreviousCamundaProcessInstanceId(originalPreviousCamundaProcessInstanceId);
            procedure.setFormData(originalFormData == null ? null : new HashMap<>(originalFormData));
            procedure.setLastAction(originalLastAction);
            procedure.setLastComment(originalLastComment);
            procedure.setResubmissionCount(originalResubmissionCount);
            procedure.setResolvedObservationBy(originalResolvedObservationBy);
            procedure.setResolvedObservationAt(originalResolvedObservationAt);
            procedure.setResolvedObservationComment(originalResolvedObservationComment);
            procedure.setCompletedAt(originalCompletedAt);
            procedure.setUpdatedAt(originalUpdatedAt);
            procedure.setCurrentTaskId(originalCurrentTaskId);
            procedure.setCurrentNodeId(originalCurrentNodeId);
            procedure.setCurrentNodeName(originalCurrentNodeName);
            procedure.setCurrentAssigneeUsername(originalCurrentAssigneeUsername);
            procedure.setClaimedAt(originalClaimedAt);
            procedure.setStatus(statusBefore);
            procedureRepository.save(procedure);
            if (restartedInstanceId != null) {
                safeDeleteProcessInstance(restartedInstanceId, "Rollback por validacion al subsanar observacion");
            }
            throw new IllegalStateException("Error al reiniciar el tramite en Camunda: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ProcedureResponse> getAll() {
        return procedureRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ProcedureResponse getById(String id) {
        return procedureRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Tramite no encontrado: " + id));
    }

    private Map<String, Object> buildProcessVariables(Procedure procedure) {
        Map<String, Object> variables = new HashMap<>();
        variables.putAll(procedure.getFormData());
        variables.put("procedureId", procedure.getId());
        variables.put("procedureCode", procedure.getCode());
        variables.put("workflowDefinitionId", procedure.getWorkflowDefinitionId());
        variables.put("applicantId", procedure.getApplicantId());
        variables.put("applicantDocumentNumber", procedure.getApplicantDocumentNumber());
        variables.put("startedBy", procedure.getStartedBy());
        return variables;
    }

    private String generateCode() {
        String code;
        do {
            code = "TR-" + LocalDateTime.now().format(CODE_DATE_FORMAT) + "-" +
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (procedureRepository.existsByCode(code));
        return code;
    }

    private String resolveApplicantName(Applicant applicant) {
        if (applicant.getType() == ApplicantType.LEGAL_ENTITY) {
            return applicant.getBusinessName();
        }
        return (trimToEmpty(applicant.getFirstName()) + " " + trimToEmpty(applicant.getLastName())).trim();
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private ProcedureResponse toResponse(Procedure p) {
        return new ProcedureResponse(
                p.getId(),
                p.getCode(),
                p.getWorkflowDefinitionId(),
                p.getWorkflowCode(),
                p.getWorkflowName(),
                p.getWorkflowVersion(),
                p.getCamundaProcessKey(),
                p.getCamundaProcessInstanceId(),
                p.getPreviousCamundaProcessInstanceId(),
                p.getApplicantId(),
                p.getApplicantDocumentNumber(),
                p.getApplicantName(),
                p.getStatus(),
                p.getCurrentTaskId(),
                p.getCurrentNodeId(),
                p.getCurrentNodeName(),
                p.getCurrentAssigneeUsername(),
                p.getClaimedAt(),
                p.getFormData(),
                p.getLastAction(),
                p.getLastComment(),
                p.getLastCompletedTaskId(),
                p.getLastCompletedNodeId(),
                p.getLastCompletedTaskName(),
                p.getLastCompletedBy(),
                p.getLastCompletedAt(),
                p.getResubmissionCount(),
                p.getResolvedObservationBy(),
                p.getResolvedObservationAt(),
                p.getResolvedObservationComment(),
                p.getStartedBy(),
                p.getStartedAt(),
                p.getCompletedAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private Task waitForFirstActiveTask(String processInstanceId) {
        for (int attempt = 0; attempt < ACTIVE_TASK_LOOKUP_ATTEMPTS; attempt++) {
            Task activeTask = taskService.createTaskQuery()
                    .processInstanceId(processInstanceId)
                    .active()
                    .listPage(0, 1)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (activeTask != null) {
                return activeTask;
            }

            try {
                Thread.sleep(ACTIVE_TASK_LOOKUP_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return null;
    }

    private void safeDeleteProcessInstance(String instanceId, String reason) {
        try {
            runtimeService.deleteProcessInstance(instanceId, reason);
        } catch (Exception ignored) {
        }
    }
}
