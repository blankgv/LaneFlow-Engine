package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.DynamicForm;
import com.laneflow.engine.modules.workflow.model.FormField;
import com.laneflow.engine.modules.workflow.model.embedded.FieldValidation;
import com.laneflow.engine.modules.workflow.model.embedded.FileConfig;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.model.enums.NodeType;
import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.repository.DynamicFormRepository;
import com.laneflow.engine.modules.workflow.repository.FormFieldRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import com.laneflow.engine.modules.workflow.request.CreateFieldRequest;
import com.laneflow.engine.modules.workflow.request.CreateFormRequest;
import com.laneflow.engine.modules.workflow.request.ReorderFieldsRequest;
import com.laneflow.engine.modules.workflow.request.UpdateFieldRequest;
import com.laneflow.engine.modules.workflow.request.UpdateFormRequest;
import com.laneflow.engine.modules.workflow.response.DynamicFormResponse;
import com.laneflow.engine.modules.workflow.response.FormFieldResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicFormServiceImpl implements DynamicFormService {

    private final DynamicFormRepository dynamicFormRepository;
    private final FormFieldRepository formFieldRepository;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowAccessService workflowAccessService;

    @Override
    public List<DynamicFormResponse> findByWorkflow(String workflowId, String username) {
        workflowAccessService.requireReadable(workflowId, username);
        return dynamicFormRepository.findByWorkflowDefinitionId(workflowId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DynamicFormResponse findByWorkflowAndNode(String workflowId, String nodeId, String username) {
        workflowAccessService.requireReadable(workflowId, username);
        DynamicForm form = dynamicFormRepository.findByWorkflowDefinitionIdAndNodeId(workflowId, nodeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Formulario no encontrado para el nodo %s del workflow %s".formatted(nodeId, workflowId)));
        return toResponse(form);
    }

    @Override
    public Optional<DynamicFormResponse> findOptionalByWorkflowAndNode(String workflowId, String nodeId) {
        return dynamicFormRepository.findByWorkflowDefinitionIdAndNodeId(workflowId, nodeId)
                .map(this::toResponse);
    }

    @Override
    public DynamicFormResponse findById(String id, String username) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado: " + id));
        workflowAccessService.requireReadable(form.getWorkflowDefinitionId(), username);
        return toResponse(form);
    }

    @Override
    public DynamicFormResponse create(CreateFormRequest request, String username) {
        WorkflowDefinition workflow = workflowAccessService.requireWritable(request.workflowDefinitionId(), username);
        WorkflowNode node = findAssignableNode(workflow, request.nodeId());

        dynamicFormRepository.findByWorkflowDefinitionIdAndNodeId(request.workflowDefinitionId(), request.nodeId())
                .ifPresent(existing -> {
                    throw new IllegalStateException("El nodo ya tiene un formulario asignado.");
                });

        DynamicForm form = DynamicForm.builder()
                .workflowDefinitionId(request.workflowDefinitionId())
                .nodeId(request.nodeId())
                .nodeName(node.getName())
                .title(request.title())
                .build();

        DynamicForm saved = dynamicFormRepository.save(form);
        node.setFormKey(saved.getId());
        workflowDefinitionRepository.save(workflow);
        log.info("Formulario creado: {} para nodo {}", saved.getTitle(), saved.getNodeId());
        return toResponse(saved);
    }

    @Override
    public DynamicFormResponse update(String id, UpdateFormRequest request, String username) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado: " + id));
        WorkflowDefinition workflow = workflowAccessService.requireWritable(form.getWorkflowDefinitionId(), username);
        WorkflowNode node = findAssignableNode(workflow, form.getNodeId());

        form.setNodeName(node.getName());
        if (request.title() != null) form.setTitle(request.title());
        form.setUpdatedAt(LocalDateTime.now());

        return toResponse(dynamicFormRepository.save(form));
    }

    @Override
    @Transactional
    public void delete(String id, String username) {
        DynamicForm form = dynamicFormRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado: " + id));
        workflowAccessService.requireWritable(form.getWorkflowDefinitionId(), username);
        workflowDefinitionRepository.findById(form.getWorkflowDefinitionId()).ifPresent(workflow -> {
            WorkflowNode node = findNode(workflow, form.getNodeId());
            if (node != null && id.equals(node.getFormKey())) {
                node.setFormKey(null);
                workflowDefinitionRepository.save(workflow);
            }
        });

        formFieldRepository.deleteByFormId(id);
        dynamicFormRepository.delete(form);
        log.info("Formulario eliminado: {}", form.getTitle());
    }

    @Override
    public FormFieldResponse addField(String formId, CreateFieldRequest request, String username) {
        DynamicForm form = dynamicFormRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado: " + formId));
        workflowAccessService.requireWritable(form.getWorkflowDefinitionId(), username);

        List<FieldValidation> validations = null;
        if (request.validations() != null) {
            validations = request.validations().stream()
                    .map(v -> FieldValidation.builder()
                            .type(v.type())
                            .value(v.value())
                            .message(v.message())
                            .build())
                    .toList();
        }

        FileConfig fileConfig = null;
        if (request.fileConfig() != null) {
            fileConfig = FileConfig.builder()
                    .allowedExtensions(request.fileConfig().allowedExtensions())
                    .maxSizeMb(request.fileConfig().maxSizeMb())
                    .multiple(request.fileConfig().multiple())
                    .bucketFolder(request.fileConfig().bucketFolder())
                    .build();
        }

        FormField field = FormField.builder()
                .formId(formId)
                .name(request.name())
                .label(request.label())
                .type(request.type())
                .required(request.required())
                .order(request.order())
                .options(request.options())
                .validations(validations)
                .fileConfig(fileConfig)
                .build();

        FormField saved = formFieldRepository.save(field);
        log.info("Campo {} agregado al formulario {}", saved.getName(), formId);
        return toFieldResponse(saved);
    }

    @Override
    public FormFieldResponse updateField(String formId, String fieldId, UpdateFieldRequest request, String username) {
        FormField field = formFieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Campo no encontrado: " + fieldId));
        DynamicForm form = dynamicFormRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado: " + formId));
        workflowAccessService.requireWritable(form.getWorkflowDefinitionId(), username);

        if (!formId.equals(field.getFormId())) {
            throw new IllegalArgumentException("El campo no pertenece al formulario: " + formId);
        }

        if (request.name() != null) field.setName(request.name());
        if (request.label() != null) field.setLabel(request.label());
        if (request.type() != null) field.setType(request.type());
        if (request.required() != null) field.setRequired(request.required());
        if (request.order() != null) field.setOrder(request.order());
        if (request.options() != null) field.setOptions(request.options());

        if (request.validations() != null) {
            field.setValidations(request.validations().stream()
                    .map(v -> FieldValidation.builder()
                            .type(v.type())
                            .value(v.value())
                            .message(v.message())
                            .build())
                    .toList());
        }

        if (request.fileConfig() != null) {
            field.setFileConfig(FileConfig.builder()
                    .allowedExtensions(request.fileConfig().allowedExtensions())
                    .maxSizeMb(request.fileConfig().maxSizeMb())
                    .multiple(request.fileConfig().multiple())
                    .bucketFolder(request.fileConfig().bucketFolder())
                    .build());
        }

        field.setUpdatedAt(LocalDateTime.now());
        return toFieldResponse(formFieldRepository.save(field));
    }

    @Override
    public void deleteField(String formId, String fieldId, String username) {
        FormField field = formFieldRepository.findById(fieldId)
                .orElseThrow(() -> new IllegalArgumentException("Campo no encontrado: " + fieldId));
        DynamicForm form = dynamicFormRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado: " + formId));
        workflowAccessService.requireWritable(form.getWorkflowDefinitionId(), username);

        if (!formId.equals(field.getFormId())) {
            throw new IllegalArgumentException("El campo no pertenece al formulario: " + formId);
        }

        formFieldRepository.delete(field);
        log.info("Campo {} eliminado del formulario {}", fieldId, formId);
    }

    @Override
    public List<FormFieldResponse> reorderFields(String formId, ReorderFieldsRequest request, String username) {
        DynamicForm form = dynamicFormRepository.findById(formId)
                .orElseThrow(() -> new IllegalArgumentException("Formulario no encontrado: " + formId));
        workflowAccessService.requireWritable(form.getWorkflowDefinitionId(), username);

        for (ReorderFieldsRequest.FieldOrderItem item : request.fields()) {
            formFieldRepository.findById(item.fieldId()).ifPresent(field -> {
                if (formId.equals(field.getFormId())) {
                    field.setOrder(item.order());
                    field.setUpdatedAt(LocalDateTime.now());
                    formFieldRepository.save(field);
                }
            });
        }

        return formFieldRepository.findByFormIdOrderByOrderAsc(formId)
                .stream()
                .map(this::toFieldResponse)
                .toList();
    }

    @Override
    public void validateNodeBindings(String workflowId, List<WorkflowNode> nodes) {
        List<DynamicForm> forms = dynamicFormRepository.findByWorkflowDefinitionId(workflowId);
        if (forms.isEmpty()) {
            return;
        }

        List<String> errors = new ArrayList<>();
        for (DynamicForm form : forms) {
            WorkflowNode node = nodes == null ? null : nodes.stream()
                    .filter(candidate -> form.getNodeId().equals(candidate.getId()))
                    .findFirst()
                    .orElse(null);

            if (node == null) {
                errors.add("El formulario '%s' quedaria huerfano porque el nodo '%s' ya no existe."
                        .formatted(form.getTitle(), form.getNodeId()));
                continue;
            }

            if (node.getType() != NodeType.USER_TASK) {
                errors.add("El formulario '%s' no puede quedar asignado al nodo '%s' porque ya no es USER_TASK."
                        .formatted(form.getTitle(), form.getNodeId()));
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalStateException(String.join(" ", errors));
        }
    }

    @Override
    public void syncNodeBindings(String workflowId, List<WorkflowNode> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        List<DynamicForm> forms = dynamicFormRepository.findByWorkflowDefinitionId(workflowId);
        if (forms.isEmpty()) {
            return;
        }

        for (WorkflowNode node : nodes) {
            if (node == null || node.getId() == null) {
                continue;
            }

            DynamicForm matchedForm = forms.stream()
                    .filter(form -> node.getId().equals(form.getNodeId()))
                    .findFirst()
                    .orElse(null);

            if (matchedForm == null) {
                continue;
            }

            node.setFormKey(matchedForm.getId());
            if (node.getName() != null && !node.getName().equals(matchedForm.getNodeName())) {
                matchedForm.setNodeName(node.getName());
                matchedForm.setUpdatedAt(LocalDateTime.now());
                dynamicFormRepository.save(matchedForm);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private WorkflowNode findAssignableNode(WorkflowDefinition workflow, String nodeId) {
        WorkflowNode node = findNode(workflow, nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Nodo no encontrado en el workflow: " + nodeId);
        }
        if (node.getType() != NodeType.USER_TASK) {
            throw new IllegalStateException("Solo se pueden asignar formularios a tareas de usuario.");
        }
        return node;
    }

    private WorkflowNode findNode(WorkflowDefinition workflow, String nodeId) {
        if (workflow.getNodes() == null) {
            return null;
        }

        return workflow.getNodes().stream()
                .filter(node -> nodeId.equals(node.getId()))
                .findFirst()
                .orElse(null);
    }

    private DynamicFormResponse toResponse(DynamicForm form) {
        List<FormFieldResponse> fields = formFieldRepository.findByFormIdOrderByOrderAsc(form.getId())
                .stream()
                .map(this::toFieldResponse)
                .toList();
        return new DynamicFormResponse(
                form.getId(),
                form.getWorkflowDefinitionId(),
                form.getNodeId(),
                form.getNodeName(),
                form.getTitle(),
                fields,
                form.getCreatedAt(),
                form.getUpdatedAt()
        );
    }

    private FormFieldResponse toFieldResponse(FormField f) {
        return new FormFieldResponse(
                f.getId(),
                f.getFormId(),
                f.getName(),
                f.getLabel(),
                f.getType(),
                f.isRequired(),
                f.getOrder(),
                f.getOptions(),
                f.getValidations(),
                f.getFileConfig(),
                f.getCreatedAt(),
                f.getUpdatedAt()
        );
    }
}
