package com.laneflow.engine.modules.workflow.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.workflow.response.WorkflowEditorResponse;
import com.laneflow.engine.modules.workflow.service.WorkflowEditorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ApiVersion.V1 + "/workflows")
public class WorkflowEditorController {

    private final WorkflowEditorService workflowEditorService;

    @GetMapping("/{workflowId}/editor")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<WorkflowEditorResponse> getEditorSnapshot(@PathVariable String workflowId) {
        return ResponseEntity.ok(workflowEditorService.getEditorSnapshot(workflowId, currentUsername()));
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}
