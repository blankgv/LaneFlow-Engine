package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.admin.model.Department;
import com.laneflow.engine.modules.admin.model.Staff;
import com.laneflow.engine.modules.admin.model.User;
import com.laneflow.engine.modules.admin.repository.DepartmentRepository;
import com.laneflow.engine.modules.admin.repository.StaffRepository;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.repository.ProcedureRepository;
import com.laneflow.engine.modules.operation.response.TaskResponse;
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
import java.util.List;
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
        procedure.setCurrentTaskId(task.getId());
        procedure.setCurrentNodeId(task.getTaskDefinitionKey());
        procedure.setCurrentNodeName(task.getName());
        procedure.setCurrentAssigneeUsername(username);
        procedure.setClaimedAt(LocalDateTime.now());
        procedure.setUpdatedAt(LocalDateTime.now());
        procedureRepository.save(procedure);

        Task claimedTask = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();
        return toResponse(claimedTask);
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

    private record ResponsibleDepartment(String departmentId, String departmentCode) {}
}
