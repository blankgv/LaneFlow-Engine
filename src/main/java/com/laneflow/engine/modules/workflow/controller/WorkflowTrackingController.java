package com.laneflow.engine.modules.workflow.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.workflow.response.WorkflowHistoryResponse;
import com.laneflow.engine.modules.workflow.service.WorkflowAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiVersion.V1 + "/workflows")
@RequiredArgsConstructor
public class WorkflowTrackingController {

    private final WorkflowAuditService workflowAuditService;

    @GetMapping("/{workflowId}/history")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<WorkflowHistoryResponse> getHistory(@PathVariable String workflowId) {
        return ResponseEntity.ok(workflowAuditService.getHistory(workflowId));
    }
}
