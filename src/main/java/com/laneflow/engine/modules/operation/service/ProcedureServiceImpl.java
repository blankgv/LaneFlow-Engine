package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.operation.model.Applicant;
import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.model.enums.ApplicantType;
import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import com.laneflow.engine.modules.operation.repository.ApplicantRepository;
import com.laneflow.engine.modules.operation.repository.ProcedureRepository;
import com.laneflow.engine.modules.operation.request.StartProcedureRequest;
import com.laneflow.engine.modules.operation.response.ProcedureResponse;
import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
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

    private final ProcedureRepository procedureRepository;
    private final ApplicantRepository applicantRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

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

        try {
            Map<String, Object> variables = buildProcessVariables(procedure);
            ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                    workflow.getCamundaProcessKey(),
                    procedure.getId(),
                    variables
            );

            Task activeTask = taskService.createTaskQuery()
                    .processInstanceId(instance.getId())
                    .active()
                    .listPage(0, 1)
                    .stream()
                    .findFirst()
                    .orElse(null);

            procedure.setCamundaProcessInstanceId(instance.getId());
            procedure.setStatus(ProcedureStatus.IN_PROGRESS);
            if (activeTask != null) {
                procedure.setCurrentNodeId(activeTask.getTaskDefinitionKey());
                procedure.setCurrentNodeName(activeTask.getName());
            }
            procedure.setUpdatedAt(LocalDateTime.now());

            return toResponse(procedureRepository.save(procedure));
        } catch (Exception e) {
            procedureRepository.delete(procedure);
            throw new IllegalStateException("Error al iniciar el tramite en Camunda: " + e.getMessage(), e);
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
                p.getApplicantId(),
                p.getApplicantDocumentNumber(),
                p.getApplicantName(),
                p.getStatus(),
                p.getCurrentNodeId(),
                p.getCurrentNodeName(),
                p.getFormData(),
                p.getStartedBy(),
                p.getStartedAt(),
                p.getCompletedAt(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
