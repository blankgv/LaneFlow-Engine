package com.laneflow.engine.modules.workflow.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.workflow.request.CreateVersionRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowVersionResponse;
import com.laneflow.engine.modules.workflow.service.WorkflowVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/workflows/{workflowId}/versions")
@RequiredArgsConstructor
public class WorkflowVersionController {

    private final WorkflowVersionService workflowVersionService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<List<WorkflowVersionResponse>> findByWorkflow(@PathVariable String workflowId) {
        return ResponseEntity.ok(workflowVersionService.findByWorkflow(workflowId, currentUsername()));
    }

    @GetMapping("/{versionNumber}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<WorkflowVersionResponse> findByWorkflowAndVersion(
            @PathVariable String workflowId,
            @PathVariable int versionNumber) {
        return ResponseEntity.ok(workflowVersionService.findByWorkflowAndVersion(workflowId, versionNumber, currentUsername()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<WorkflowVersionResponse> createDraft(
            @PathVariable String workflowId,
            @Valid @RequestBody CreateVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowVersionService.createDraft(workflowId, request, currentUsername()));
    }

    @PostMapping("/{versionNumber}/publish")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<WorkflowVersionResponse> publish(
            @PathVariable String workflowId,
            @PathVariable int versionNumber) {
        return ResponseEntity.ok(workflowVersionService.publish(workflowId, versionNumber, currentUsername()));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
