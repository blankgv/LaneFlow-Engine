package com.laneflow.engine.modules.workflow.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.workflow.request.CreateWorkflowInvitationRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowCollaboratorResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowInviteeResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowInvitationResponse;
import com.laneflow.engine.modules.workflow.service.WorkflowCollaborationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WorkflowCollaborationController {

    private final WorkflowCollaborationService workflowCollaborationService;

    @GetMapping(ApiVersion.V1 + "/workflows/{workflowId}/collaborators")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<List<WorkflowCollaboratorResponse>> findCollaborators(@PathVariable String workflowId) {
        return ResponseEntity.ok(workflowCollaborationService.findCollaborators(workflowId, currentUsername()));
    }

    @GetMapping(ApiVersion.V1 + "/workflows/{workflowId}/invitees")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<List<WorkflowInviteeResponse>> findEligibleInvitees(@PathVariable String workflowId) {
        return ResponseEntity.ok(workflowCollaborationService.findEligibleInvitees(workflowId, currentUsername()));
    }

    @GetMapping(ApiVersion.V1 + "/workflows/{workflowId}/invitations")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<List<WorkflowInvitationResponse>> findInvitationsByWorkflow(@PathVariable String workflowId) {
        return ResponseEntity.ok(workflowCollaborationService.findInvitationsByWorkflow(workflowId, currentUsername()));
    }

    @GetMapping(ApiVersion.V1 + "/workflow-invitations/mine")
    public ResponseEntity<List<WorkflowInvitationResponse>> findMyInvitations() {
        return ResponseEntity.ok(workflowCollaborationService.findMyInvitations(currentUsername()));
    }

    @PostMapping(ApiVersion.V1 + "/workflows/{workflowId}/invitations")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<WorkflowInvitationResponse> invite(
            @PathVariable String workflowId,
            @Valid @RequestBody CreateWorkflowInvitationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowCollaborationService.invite(workflowId, request, currentUsername()));
    }

    @PostMapping(ApiVersion.V1 + "/workflow-invitations/{invitationId}/accept")
    public ResponseEntity<WorkflowInvitationResponse> accept(@PathVariable String invitationId) {
        return ResponseEntity.ok(workflowCollaborationService.accept(invitationId, currentUsername()));
    }

    @PostMapping(ApiVersion.V1 + "/workflow-invitations/{invitationId}/reject")
    public ResponseEntity<WorkflowInvitationResponse> reject(@PathVariable String invitationId) {
        return ResponseEntity.ok(workflowCollaborationService.reject(invitationId, currentUsername()));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
