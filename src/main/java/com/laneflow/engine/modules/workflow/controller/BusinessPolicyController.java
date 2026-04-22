package com.laneflow.engine.modules.workflow.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.workflow.request.CreatePolicyRequest;
import com.laneflow.engine.modules.workflow.request.UpdatePolicyRequest;
import com.laneflow.engine.modules.workflow.response.BusinessPolicyResponse;
import com.laneflow.engine.modules.workflow.service.BusinessPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/policies")
@RequiredArgsConstructor
public class BusinessPolicyController {

    private final BusinessPolicyService businessPolicyService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<List<BusinessPolicyResponse>> findAll(@RequestParam String workflowId) {
        return ResponseEntity.ok(businessPolicyService.findByWorkflow(workflowId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<BusinessPolicyResponse> create(@Valid @RequestBody CreatePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(businessPolicyService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<BusinessPolicyResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(businessPolicyService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<BusinessPolicyResponse> update(@PathVariable String id,
                                                          @Valid @RequestBody UpdatePolicyRequest request) {
        return ResponseEntity.ok(businessPolicyService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        businessPolicyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<BusinessPolicyResponse> toggleActive(@PathVariable String id) {
        return ResponseEntity.ok(businessPolicyService.toggleActive(id));
    }
}
