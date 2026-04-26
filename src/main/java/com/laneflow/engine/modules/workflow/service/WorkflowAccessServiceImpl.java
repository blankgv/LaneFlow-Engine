package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.admin.model.Role;
import com.laneflow.engine.modules.admin.model.User;
import com.laneflow.engine.modules.admin.repository.RoleRepository;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.repository.WorkflowCollaboratorRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowAccessServiceImpl implements WorkflowAccessService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowCollaboratorRepository workflowCollaboratorRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public WorkflowDefinition requireReadable(String workflowId, String username) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + workflowId));
        requireReadable(workflow, username);
        return workflow;
    }

    @Override
    public WorkflowDefinition requireWritable(String workflowId, String username) {
        WorkflowDefinition workflow = workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + workflowId));
        requireWritable(workflow, username);
        return workflow;
    }

    @Override
    public void requireReadable(WorkflowDefinition workflow, String username) {
        if (!canRead(workflow, username)) {
            throw new IllegalStateException("El usuario no tiene acceso a esta politica.");
        }
    }

    @Override
    public void requireWritable(WorkflowDefinition workflow, String username) {
        if (!canWrite(workflow, username)) {
            throw new IllegalStateException("El usuario no puede modificar esta politica.");
        }
    }

    @Override
    public boolean canRead(WorkflowDefinition workflow, String username) {
        return hasAccess(workflow, username, Permission.WORKFLOW_READ);
    }

    @Override
    public boolean canWrite(WorkflowDefinition workflow, String username) {
        return hasAccess(workflow, username, Permission.WORKFLOW_WRITE);
    }

    @Override
    public List<WorkflowDefinition> filterReadable(List<WorkflowDefinition> workflows, String username) {
        return workflows.stream()
                .filter(workflow -> canRead(workflow, username))
                .toList();
    }

    private boolean hasAccess(WorkflowDefinition workflow, String username, String requiredPermission) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + user.getRoleId()));

        if (!user.isActive() || !role.isActive() || role.getPermissions() == null) {
            return false;
        }

        if (role.getPermissions().containsAll(Permission.ALL)) {
            return true;
        }

        boolean hasGlobalPermission = role.getPermissions().contains(requiredPermission);
        if (!hasGlobalPermission) {
            return false;
        }

        if (username.equals(workflow.getCreatedBy())) {
            return true;
        }

        return workflowCollaboratorRepository.existsByWorkflowDefinitionIdAndUserId(workflow.getId(), user.getId());
    }
}
