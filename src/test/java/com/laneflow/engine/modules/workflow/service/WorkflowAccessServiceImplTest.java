package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.admin.model.Role;
import com.laneflow.engine.modules.admin.model.User;
import com.laneflow.engine.modules.admin.repository.RoleRepository;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.repository.WorkflowCollaboratorRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowAccessServiceImplTest {

    private final WorkflowDefinitionRepository workflowDefinitionRepository = mock(WorkflowDefinitionRepository.class);
    private final WorkflowCollaboratorRepository workflowCollaboratorRepository = mock(WorkflowCollaboratorRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final WorkflowAccessServiceImpl service = new WorkflowAccessServiceImpl(
            workflowDefinitionRepository,
            workflowCollaboratorRepository,
            userRepository,
            roleRepository
    );

    @Test
    void collaboratorCanReadButOutsiderCannot() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("wf-1")
                .createdBy("admin")
                .build();
        User collaborator = User.builder().id("user-1").username("collab").roleId("role-1").active(true).build();
        User outsider = User.builder().id("user-2").username("outsider").roleId("role-1").active(true).build();
        Role role = Role.builder()
                .id("role-1")
                .active(true)
                .permissions(List.of(Permission.WORKFLOW_READ, Permission.WORKFLOW_WRITE))
                .build();

        when(userRepository.findByUsername("collab")).thenReturn(Optional.of(collaborator));
        when(userRepository.findByUsername("outsider")).thenReturn(Optional.of(outsider));
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(role));
        when(workflowCollaboratorRepository.existsByWorkflowDefinitionIdAndUserId("wf-1", "user-1")).thenReturn(true);
        when(workflowCollaboratorRepository.existsByWorkflowDefinitionIdAndUserId("wf-1", "user-2")).thenReturn(false);

        assertTrue(service.canRead(workflow, "collab"));
        assertFalse(service.canRead(workflow, "outsider"));
        assertThrows(IllegalStateException.class, () -> service.requireReadable(workflow, "outsider"));
    }

    @Test
    void ownerWithWritePermissionCanModifyWorkflow() {
        WorkflowDefinition workflow = WorkflowDefinition.builder()
                .id("wf-1")
                .createdBy("owner")
                .build();
        User owner = User.builder().id("user-1").username("owner").roleId("role-1").active(true).build();
        Role role = Role.builder()
                .id("role-1")
                .active(true)
                .permissions(List.of(Permission.WORKFLOW_READ, Permission.WORKFLOW_WRITE))
                .build();

        when(userRepository.findByUsername("owner")).thenReturn(Optional.of(owner));
        when(roleRepository.findById("role-1")).thenReturn(Optional.of(role));

        assertTrue(service.canWrite(workflow, "owner"));
    }
}
