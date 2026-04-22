package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.request.CreateFieldRequest;
import com.laneflow.engine.modules.workflow.request.CreateFormRequest;
import com.laneflow.engine.modules.workflow.request.ReorderFieldsRequest;
import com.laneflow.engine.modules.workflow.request.UpdateFieldRequest;
import com.laneflow.engine.modules.workflow.request.UpdateFormRequest;
import com.laneflow.engine.modules.workflow.response.DynamicFormResponse;
import com.laneflow.engine.modules.workflow.response.FormFieldResponse;

import java.util.List;

public interface DynamicFormService {

    List<DynamicFormResponse> findByWorkflow(String workflowId);

    DynamicFormResponse findById(String id);

    DynamicFormResponse create(CreateFormRequest request);

    DynamicFormResponse update(String id, UpdateFormRequest request);

    void delete(String id);

    FormFieldResponse addField(String formId, CreateFieldRequest request);

    FormFieldResponse updateField(String formId, String fieldId, UpdateFieldRequest request);

    void deleteField(String formId, String fieldId);

    List<FormFieldResponse> reorderFields(String formId, ReorderFieldsRequest request);
}
