package com.laneflow.engine.modules.workflow.controller;

import com.laneflow.engine.core.common.ApiVersion;
import com.laneflow.engine.core.common.Permission;
import com.laneflow.engine.modules.workflow.request.CreateFieldRequest;
import com.laneflow.engine.modules.workflow.request.CreateFormRequest;
import com.laneflow.engine.modules.workflow.request.ReorderFieldsRequest;
import com.laneflow.engine.modules.workflow.request.UpdateFieldRequest;
import com.laneflow.engine.modules.workflow.request.UpdateFormRequest;
import com.laneflow.engine.modules.workflow.response.DynamicFormResponse;
import com.laneflow.engine.modules.workflow.response.FormFieldResponse;
import com.laneflow.engine.modules.workflow.service.DynamicFormService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiVersion.V1 + "/forms")
@RequiredArgsConstructor
public class DynamicFormController {

    private final DynamicFormService dynamicFormService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<List<DynamicFormResponse>> findByWorkflow(@RequestParam String workflowId) {
        return ResponseEntity.ok(dynamicFormService.findByWorkflow(workflowId));
    }

    @GetMapping("/by-node")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<DynamicFormResponse> findByWorkflowAndNode(
            @RequestParam String workflowId,
            @RequestParam String nodeId
    ) {
        return ResponseEntity.ok(dynamicFormService.findByWorkflowAndNode(workflowId, nodeId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<DynamicFormResponse> create(@Valid @RequestBody CreateFormRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dynamicFormService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_READ + "')")
    public ResponseEntity<DynamicFormResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(dynamicFormService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<DynamicFormResponse> update(@PathVariable String id,
                                                       @Valid @RequestBody UpdateFormRequest request) {
        return ResponseEntity.ok(dynamicFormService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        dynamicFormService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{formId}/fields")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<FormFieldResponse> addField(@PathVariable String formId,
                                                       @Valid @RequestBody CreateFieldRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dynamicFormService.addField(formId, request));
    }

    @PutMapping("/{formId}/fields/{fieldId}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<FormFieldResponse> updateField(@PathVariable String formId,
                                                          @PathVariable String fieldId,
                                                          @Valid @RequestBody UpdateFieldRequest request) {
        return ResponseEntity.ok(dynamicFormService.updateField(formId, fieldId, request));
    }

    @DeleteMapping("/{formId}/fields/{fieldId}")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<Void> deleteField(@PathVariable String formId,
                                             @PathVariable String fieldId) {
        dynamicFormService.deleteField(formId, fieldId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{formId}/fields/reorder")
    @PreAuthorize("hasAuthority('" + Permission.WORKFLOW_WRITE + "')")
    public ResponseEntity<List<FormFieldResponse>> reorderFields(@PathVariable String formId,
                                                                  @Valid @RequestBody ReorderFieldsRequest request) {
        return ResponseEntity.ok(dynamicFormService.reorderFields(formId, request));
    }
}
