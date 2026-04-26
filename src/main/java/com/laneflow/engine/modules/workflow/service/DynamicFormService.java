package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.request.CreateFieldRequest;
import com.laneflow.engine.modules.workflow.request.CreateFormRequest;
import com.laneflow.engine.modules.workflow.request.ReorderFieldsRequest;
import com.laneflow.engine.modules.workflow.request.UpdateFieldRequest;
import com.laneflow.engine.modules.workflow.request.UpdateFormRequest;
import com.laneflow.engine.modules.workflow.response.DynamicFormResponse;
import com.laneflow.engine.modules.workflow.response.FormFieldResponse;

import java.util.List;
import java.util.Optional;

public interface DynamicFormService {

    List<DynamicFormResponse> findByWorkflow(String workflowId, String username);

    DynamicFormResponse findByWorkflowAndNode(String workflowId, String nodeId, String username);

    Optional<DynamicFormResponse> findOptionalByWorkflowAndNode(String workflowId, String nodeId);

    DynamicFormResponse findById(String id, String username);

    DynamicFormResponse create(CreateFormRequest request, String username);

    DynamicFormResponse update(String id, UpdateFormRequest request, String username);

    void delete(String id, String username);

    FormFieldResponse addField(String formId, CreateFieldRequest request, String username);

    FormFieldResponse updateField(String formId, String fieldId, UpdateFieldRequest request, String username);

    void deleteField(String formId, String fieldId, String username);

    List<FormFieldResponse> reorderFields(String formId, ReorderFieldsRequest request, String username);

    void validateNodeBindings(String workflowId, List<WorkflowNode> nodes);

    void syncNodeBindings(String workflowId, List<WorkflowNode> nodes);
}
