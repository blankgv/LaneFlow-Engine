package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.admin.model.Department;
import com.laneflow.engine.modules.admin.model.Staff;
import com.laneflow.engine.modules.admin.model.User;
import com.laneflow.engine.modules.admin.repository.DepartmentRepository;
import com.laneflow.engine.modules.admin.repository.StaffRepository;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import com.laneflow.engine.modules.operation.model.enums.TaskAction;
import com.laneflow.engine.modules.operation.repository.ProcedureRepository;
import com.laneflow.engine.modules.operation.request.ApproveTaskRequest;
import com.laneflow.engine.modules.operation.request.CompleteTaskRequest;
import com.laneflow.engine.modules.operation.request.ObserveTaskRequest;
import com.laneflow.engine.modules.operation.request.RejectTaskRequest;
import com.laneflow.engine.modules.operation.response.ProcedureResponse;
import com.laneflow.engine.modules.operation.response.TaskResponse;
import com.laneflow.engine.modules.tracking.model.enums.ProcedureAuditAction;
import com.laneflow.engine.modules.tracking.service.ProcedureAuditService;
import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.embedded.Swimlane;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final org.camunda.bpm.engine.TaskService camundaTaskService;
    private final RuntimeService runtimeService;
    private final ProcedureRepository procedureRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final UserRepository userRepository;
    private final StaffRepository staffRepository;
    private final DepartmentRepository departmentRepository;
    private final ProcedureAuditService procedureAuditService;

    @Override
    public List<TaskResponse> getAvailable(String username) {
        Staff staff = currentStaff(username);
        return camundaTaskService.createTaskQuery()
                .active()
                .taskUnassigned()
                .list()
                .stream()
                .filter(task -> canClaim(task, staff))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<TaskResponse> getMine(String username) {
        return camundaTaskService.createTaskQuery()
                .active()
                .taskAssignee(username)
                .list()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public TaskResponse claim(String taskId, String username) {
        Task task = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .active()
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Tarea no encontrada o no activa: " + taskId);
        }

        if (task.getAssignee() != null && !task.getAssignee().isBlank()) {
            throw new IllegalStateException("La tarea ya fue tomada por: " + task.getAssignee());
        }

        Staff staff = currentStaff(username);
        if (!canClaim(task, staff)) {
            throw new IllegalStateException("El usuario no pertenece al departamento responsable de la tarea.");
        }

        camundaTaskService.claim(taskId, username);

        Procedure procedure = resolveProcedure(task);
        ProcedureStatus statusBefore = procedure.getStatus();
        procedure.setCurrentTaskId(task.getId());
        procedure.setCurrentNodeId(task.getTaskDefinitionKey());
        procedure.setCurrentNodeName(task.getName());
        procedure.setCurrentAssigneeUsername(username);
        procedure.setClaimedAt(LocalDateTime.now());
        procedure.setUpdatedAt(LocalDateTime.now());
        Procedure saved = procedureRepository.save(procedure);
        procedureAuditService.record(
                saved,
                ProcedureAuditAction.TASK_CLAIMED,
                "Tarea tomada desde la cola.",
                username,
                task.getId(),
                task.getTaskDefinitionKey(),
                task.getName(),
                statusBefore,
                saved.getStatus(),
                Map.of("assignee", username)
        );

        Task claimedTask = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        return toResponse(claimedTask);
    }

    @Override
    public ProcedureResponse complete(String taskId, CompleteTaskRequest request, String username) {
        Task task = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .active()
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("Tarea no encontrada o no activa: " + taskId);
        }

        if (task.getAssignee() == null || task.getAssignee().isBlank()) {
            throw new IllegalStateException("La tarea debe ser tomada antes de ejecutarla.");
        }

        if (!username.equals(task.getAssignee())) {
            throw new IllegalStateException("La tarea esta asignada a otro usuario: " + task.getAssignee());
        }

        Procedure procedure = resolveProcedure(task);
        ProcedureStatus statusBefore = procedure.getStatus();
        Map<String, Object> mergedFormData = new HashMap<>();
        if (procedure.getFormData() != null) {
            mergedFormData.putAll(procedure.getFormData());
        }
        if (request.formData() != null) {
            mergedFormData.putAll(request.formData());
        }

        Map<String, Object> variables = new HashMap<>();
        variables.putAll(mergedFormData);
        variables.put("procedureId", procedure.getId());
        variables.put("procedureCode", procedure.getCode());
        variables.put("lastAction", request.action().name());
        variables.put("lastComment", request.comment());
        variables.put("lastCompletedBy", username);

        camundaTaskService.complete(taskId, variables);

        procedure.setFormData(mergedFormData);
        procedure.setLastAction(request.action().name());
        procedure.setLastComment(trimToNull(request.comment()));
        procedure.setLastCompletedTaskId(task.getId());
        procedure.setLastCompletedNodeId(task.getTaskDefinitionKey());
        procedure.setLastCompletedTaskName(task.getName());
        procedure.setLastCompletedBy(username);
        procedure.setLastCompletedAt(LocalDateTime.now());
        procedure.setUpdatedAt(LocalDateTime.now());

        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();

        if (request.action() == TaskAction.OBSERVE) {
            procedure.setStatus(ProcedureStatus.OBSERVED);
            procedure.setCompletedAt(null);
            clearCurrentTask(procedure);
        } else if (request.action() == TaskAction.REJECT) {
            procedure.setStatus(ProcedureStatus.REJECTED);
            procedure.setCompletedAt(null);
            clearCurrentTask(procedure);
        } else if (instance == null || instance.isEnded()) {
            procedure.setStatus(ProcedureStatus.COMPLETED);
            procedure.setCompletedAt(LocalDateTime.now());
            clearCurrentTask(procedure);
        } else {
            Task nextTask = camundaTaskService.createTaskQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .active()
                    .listPage(0, 1)
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (nextTask != null) {
                procedure.setStatus(ProcedureStatus.IN_PROGRESS);
                procedure.setCurrentTaskId(nextTask.getId());
                procedure.setCurrentNodeId(nextTask.getTaskDefinitionKey());
                procedure.setCurrentNodeName(nextTask.getName());
                procedure.setCurrentAssigneeUsername(nextTask.getAssignee());
                procedure.setClaimedAt(null);
            } else {
                procedure.setStatus(ProcedureStatus.IN_PROGRESS);
                clearCurrentTask(procedure);
            }
        }

        Procedure saved = procedureRepository.save(procedure);
        procedureAuditService.record(
                saved,
                resolveAuditAction(request.action()),
                resolveAuditDescription(request.action()),
                username,
                task.getId(),
                task.getTaskDefinitionKey(),
                task.getName(),
                statusBefore,
                saved.getStatus(),
                Map.of(
                        "comment", request.comment() == null ? "" : request.comment(),
                        "lastAction", request.action().name()
                )
        );
        return toProcedureResponse(saved);
    }

    @Override
    public ProcedureResponse approve(String taskId, ApproveTaskRequest request, String username) {
        CompleteTaskRequest completeRequest = new CompleteTaskRequest(
                TaskAction.APPROVE,
                request.comment(),
                request.formData()
        );
        return complete(taskId, completeRequest, username);
    }

    @Override
    public ProcedureResponse observe(String taskId, ObserveTaskRequest request, String username) {
        CompleteTaskRequest completeRequest = new CompleteTaskRequest(
                TaskAction.OBSERVE,
                request.comment(),
                request.formData()
        );
        return complete(taskId, completeRequest, username);
    }

    @Override
    public ProcedureResponse reject(String taskId, RejectTaskRequest request, String username) {
        CompleteTaskRequest completeRequest = new CompleteTaskRequest(
                TaskAction.REJECT,
                request.comment(),
                request.formData()
        );
        return complete(taskId, completeRequest, username);
    }

    private boolean canClaim(Task task, Staff staff) {
        ResponsibleDepartment responsible = resolveResponsibleDepartment(task);
        return responsible.departmentId() != null && responsible.departmentId().equals(staff.getDepartmentId());
    }

    private Staff currentStaff(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));

        if (user.getStaffId() == null || user.getStaffId().isBlank()) {
            throw new IllegalStateException("El usuario no tiene personal asociado.");
        }

        Staff staff = staffRepository.findById(user.getStaffId())
                .orElseThrow(() -> new IllegalArgumentException("Personal no encontrado para usuario: " + username));

        if (!staff.isActive()) {
            throw new IllegalStateException("El personal asociado al usuario esta inactivo.");
        }

        if (staff.getDepartmentId() == null || staff.getDepartmentId().isBlank()) {
            throw new IllegalStateException("El personal asociado al usuario no tiene departamento.");
        }

        return staff;
    }

    private ResponsibleDepartment resolveResponsibleDepartment(Task task) {
        Procedure procedure = resolveProcedure(task);
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(procedure.getWorkflowDefinitionId())
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + procedure.getWorkflowDefinitionId()));

        WorkflowNode node = workflow.getNodes() == null ? null : workflow.getNodes().stream()
                .filter(n -> task.getTaskDefinitionKey().equals(n.getId()))
                .findFirst()
                .orElse(null);

        if (node == null) {
            return new ResponsibleDepartment(null, null);
        }

        if (node.getDepartmentId() != null && !node.getDepartmentId().isBlank()) {
            return new ResponsibleDepartment(node.getDepartmentId(), null);
        }

        Swimlane swimlane = workflow.getSwimlanes() == null ? null : workflow.getSwimlanes().stream()
                .filter(s -> node.getSwimlaneId() != null && node.getSwimlaneId().equals(s.getId()))
                .findFirst()
                .orElse(null);

        if (swimlane == null) {
            return new ResponsibleDepartment(null, null);
        }

        if (swimlane.getDepartmentId() != null && !swimlane.getDepartmentId().isBlank()) {
            return new ResponsibleDepartment(swimlane.getDepartmentId(), swimlane.getDepartmentCode());
        }

        if (swimlane.getDepartmentCode() != null && !swimlane.getDepartmentCode().isBlank()) {
            Optional<Department> department = departmentRepository.findByCode(swimlane.getDepartmentCode());
            if (department.isPresent()) {
                return new ResponsibleDepartment(department.get().getId(), department.get().getCode());
            }
        }

        return new ResponsibleDepartment(null, swimlane.getDepartmentCode());
    }

    private Procedure resolveProcedure(Task task) {
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult();

        if (instance != null && instance.getBusinessKey() != null && !instance.getBusinessKey().isBlank()) {
            return procedureRepository.findById(instance.getBusinessKey())
                    .orElseThrow(() -> new IllegalArgumentException("Tramite no encontrado: " + instance.getBusinessKey()));
        }

        return procedureRepository.findByCamundaProcessInstanceId(task.getProcessInstanceId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tramite no encontrado para instancia Camunda: " + task.getProcessInstanceId()));
    }

    private TaskResponse toResponse(Task task) {
        Procedure procedure = resolveProcedure(task);
        ResponsibleDepartment responsible = resolveResponsibleDepartment(task);
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getTaskDefinitionKey(),
                task.getProcessInstanceId(),
                procedure.getId(),
                procedure.getCode(),
                procedure.getWorkflowDefinitionId(),
                procedure.getWorkflowName(),
                procedure.getApplicantId(),
                procedure.getApplicantName(),
                task.getAssignee(),
                responsible.departmentId(),
                responsible.departmentCode(),
                task.getCreateTime() == null ? null : LocalDateTime.ofInstant(
                        task.getCreateTime().toInstant(),
                        ZoneId.systemDefault()
                )
        );
    }

    private ProcedureAuditAction resolveAuditAction(TaskAction action) {
        return switch (action) {
            case COMPLETE -> ProcedureAuditAction.TASK_COMPLETED;
            case APPROVE -> ProcedureAuditAction.TASK_APPROVED;
            case OBSERVE -> ProcedureAuditAction.TASK_OBSERVED;
            case REJECT -> ProcedureAuditAction.TASK_REJECTED;
            default -> ProcedureAuditAction.TASK_COMPLETED;
        };
    }

    private String resolveAuditDescription(TaskAction action) {
        return switch (action) {
            case COMPLETE -> "Tarea completada.";
            case APPROVE -> "Tarea aprobada.";
            case OBSERVE -> "Tarea observada.";
            case REJECT -> "Tarea rechazada.";
            default -> "Tarea ejecutada.";
        };
    }

    private ProcedureResponse toProcedureResponse(Procedure p) {
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

    private void clearCurrentTask(Procedure procedure) {
        procedure.setCurrentTaskId(null);
        procedure.setCurrentNodeId(null);
        procedure.setCurrentNodeName(null);
        procedure.setCurrentAssigneeUsername(null);
        procedure.setClaimedAt(null);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private record ResponsibleDepartment(String departmentId, String departmentCode) {}
}
