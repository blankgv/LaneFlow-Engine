package com.laneflow.engine.modules.workflow.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.workflow.request.CreateWorkflowRequest;
import com.laneflow.engine.modules.workflow.request.UpdateWorkflowRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowSummaryResponse;
import com.laneflow.engine.modules.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<List<WorkflowSummaryResponse>> findAll() {
        return ResponseEntity.ok(workflowService.findAll(currentUsername()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<WorkflowResponse> create(@Valid @RequestBody CreateWorkflowRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(workflowService.create(request, currentUsername()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<WorkflowResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.findById(id, currentUsername()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<WorkflowResponse> update(@PathVariable String id,
                                                    @Valid @RequestBody UpdateWorkflowRequest request) {
        return ResponseEntity.ok(workflowService.update(id, request, currentUsername()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        workflowService.delete(id, currentUsername());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<WorkflowResponse> publish(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.publish(id, currentUsername()));
    }

    @GetMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<WorkflowResponse> validate(@PathVariable String id) {
        return ResponseEntity.ok(workflowService.validate(id, currentUsername()));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
