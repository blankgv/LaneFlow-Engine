package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.admin.model.Role;
import com.laneflow.engine.modules.admin.model.User;
import com.laneflow.engine.modules.admin.repository.RoleRepository;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.workflow.model.WorkflowCollaborator;
import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.WorkflowInvitation;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowInvitationStatus;
import com.laneflow.engine.modules.workflow.repository.WorkflowCollaboratorRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowInvitationRepository;
import com.laneflow.engine.modules.workflow.request.CreateWorkflowInvitationRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowCollaboratorResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowInviteeResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowInvitationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowCollaborationServiceImpl implements WorkflowCollaborationService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowCollaboratorRepository workflowCollaboratorRepository;
    private final WorkflowInvitationRepository workflowInvitationRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public List<WorkflowCollaboratorResponse> findCollaborators(String workflowId) {
        ensureWorkflowExists(workflowId);
        return workflowCollaboratorRepository.findByWorkflowDefinitionIdOrderByCreatedAtAsc(workflowId)
                .stream()
                .map(this::toCollaboratorResponse)
                .toList();
    }

    @Override
    public List<WorkflowInviteeResponse> findEligibleInvitees(String workflowId, String currentUsername) {
        ensureWorkflowExists(workflowId);
        User currentUser = findUserByUsername(currentUsername);

        Set<String> excludedUserIds = new HashSet<>();
        excludedUserIds.add(currentUser.getId());
        workflowCollaboratorRepository.findByWorkflowDefinitionIdOrderByCreatedAtAsc(workflowId)
                .forEach(collaborator -> excludedUserIds.add(collaborator.getUserId()));
        workflowInvitationRepository.findByWorkflowDefinitionIdOrderByCreatedAtDesc(workflowId).stream()
                .filter(invitation -> invitation.getStatus() == WorkflowInvitationStatus.PENDING)
                .forEach(invitation -> excludedUserIds.add(invitation.getInvitedUserId()));

        return userRepository.findByActiveTrueOrderByUsernameAsc().stream()
                .filter(user -> !excludedUserIds.contains(user.getId()))
                .filter(this::hasWorkflowAccess)
                .map(this::toInviteeResponse)
                .toList();
    }

    @Override
    public List<WorkflowInvitationResponse> findInvitationsByWorkflow(String workflowId) {
        ensureWorkflowExists(workflowId);
        return workflowInvitationRepository.findByWorkflowDefinitionIdOrderByCreatedAtDesc(workflowId)
                .stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Override
    public List<WorkflowInvitationResponse> findMyInvitations(String username) {
        User invitedUser = findUserByUsername(username);
        return workflowInvitationRepository.findByInvitedUserIdOrderByCreatedAtDesc(invitedUser.getId())
                .stream()
                .map(this::toInvitationResponse)
                .toList();
    }

    @Override
    public WorkflowInvitationResponse invite(String workflowId, CreateWorkflowInvitationRequest request, String invitedByUsername) {
        WorkflowDefinition workflow = ensureWorkflowExists(workflowId);
        User invitedBy = findUserByUsername(invitedByUsername);
        User invitedUser = findUserByUsername(request.invitedUsername());

        if (!invitedUser.isActive()) {
            throw new IllegalStateException("No se puede invitar a un usuario inactivo.");
        }

        if (!hasWorkflowAccess(invitedUser)) {
            throw new IllegalStateException("Solo se puede invitar a usuarios con permisos sobre workflow.");
        }

        if (invitedBy.getId().equals(invitedUser.getId())) {
            throw new IllegalArgumentException("No puedes invitarte a ti mismo.");
        }

        if (workflowCollaboratorRepository.existsByWorkflowDefinitionIdAndUserId(workflowId, invitedUser.getId())) {
            throw new IllegalStateException("El usuario ya es colaborador de esta política.");
        }

        workflowInvitationRepository
                .findByWorkflowDefinitionIdAndInvitedUserIdAndStatus(workflowId, invitedUser.getId(), WorkflowInvitationStatus.PENDING)
                .ifPresent(invitation -> {
                    throw new IllegalStateException("Ya existe una invitación pendiente para este usuario.");
                });

        WorkflowInvitation invitation = WorkflowInvitation.builder()
                .workflowDefinitionId(workflow.getId())
                .invitedUserId(invitedUser.getId())
                .invitedByUserId(invitedBy.getId())
                .status(WorkflowInvitationStatus.PENDING)
                .build();

        WorkflowInvitation saved = workflowInvitationRepository.save(invitation);
        log.info("Invitación {} creada para workflow {}", saved.getId(), workflow.getCode());
        return toInvitationResponse(saved);
    }

    @Override
    public WorkflowInvitationResponse accept(String invitationId, String username) {
        User invitedUser = findUserByUsername(username);
        WorkflowInvitation invitation = findInvitationForUser(invitationId, invitedUser.getId());

        if (invitation.getStatus() != WorkflowInvitationStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden aceptar invitaciones pendientes.");
        }

        invitation.setStatus(WorkflowInvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        workflowInvitationRepository.save(invitation);

        if (!workflowCollaboratorRepository.existsByWorkflowDefinitionIdAndUserId(
                invitation.getWorkflowDefinitionId(),
                invitedUser.getId()
        )) {
            workflowCollaboratorRepository.save(WorkflowCollaborator.builder()
                    .workflowDefinitionId(invitation.getWorkflowDefinitionId())
                    .userId(invitedUser.getId())
                    .addedBy(invitation.getInvitedByUserId())
                    .build());
        }

        return toInvitationResponse(invitation);
    }

    @Override
    public WorkflowInvitationResponse reject(String invitationId, String username) {
        User invitedUser = findUserByUsername(username);
        WorkflowInvitation invitation = findInvitationForUser(invitationId, invitedUser.getId());

        if (invitation.getStatus() != WorkflowInvitationStatus.PENDING) {
            throw new IllegalStateException("Solo se pueden rechazar invitaciones pendientes.");
        }

        invitation.setStatus(WorkflowInvitationStatus.REJECTED);
        invitation.setRespondedAt(LocalDateTime.now());
        return toInvitationResponse(workflowInvitationRepository.save(invitation));
    }

    private WorkflowDefinition ensureWorkflowExists(String workflowId) {
        return workflowDefinitionRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + workflowId));
    }

    private User findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + username));
    }

    private WorkflowInvitation findInvitationForUser(String invitationId, String userId) {
        WorkflowInvitation invitation = workflowInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new IllegalArgumentException("Invitación no encontrada: " + invitationId));

        if (!userId.equals(invitation.getInvitedUserId())) {
            throw new IllegalStateException("La invitación no pertenece al usuario autenticado.");
        }

        return invitation;
    }

    private boolean hasWorkflowAccess(User user) {
        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + user.getRoleId()));

        return role.isActive()
                && role.getPermissions() != null
                && (role.getPermissions().contains(Permission.WORKFLOW_READ)
                || role.getPermissions().contains(Permission.WORKFLOW_WRITE)
                || role.getPermissions().containsAll(List.of(Permission.WORKFLOW_READ, Permission.WORKFLOW_WRITE)));
    }

    private WorkflowCollaboratorResponse toCollaboratorResponse(WorkflowCollaborator collaborator) {
        User user = userRepository.findById(collaborator.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + collaborator.getUserId()));
        return new WorkflowCollaboratorResponse(
                collaborator.getId(),
                collaborator.getWorkflowDefinitionId(),
                collaborator.getUserId(),
                user.getUsername(),
                user.getEmail(),
                collaborator.getAddedBy(),
                collaborator.getCreatedAt()
        );
    }

    private WorkflowInvitationResponse toInvitationResponse(WorkflowInvitation invitation) {
        User invitedUser = userRepository.findById(invitation.getInvitedUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + invitation.getInvitedUserId()));
        User invitedByUser = userRepository.findById(invitation.getInvitedByUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + invitation.getInvitedByUserId()));
        return new WorkflowInvitationResponse(
                invitation.getId(),
                invitation.getWorkflowDefinitionId(),
                invitation.getInvitedUserId(),
                invitedUser.getUsername(),
                invitedUser.getEmail(),
                invitation.getInvitedByUserId(),
                invitedByUser.getUsername(),
                invitation.getStatus(),
                invitation.getCreatedAt(),
                invitation.getRespondedAt()
        );
    }

    private WorkflowInviteeResponse toInviteeResponse(User user) {
        Role role = roleRepository.findById(user.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + user.getRoleId()));
        return new WorkflowInviteeResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                role.getId(),
                role.getCode(),
                role.getName()
        );
    }
}
