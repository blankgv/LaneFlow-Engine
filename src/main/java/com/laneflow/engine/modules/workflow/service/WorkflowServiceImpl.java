package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.WorkflowVersion;
import com.laneflow.engine.modules.workflow.model.embedded.Swimlane;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowTransition;
import com.laneflow.engine.modules.workflow.model.enums.NodeType;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowAuditAction;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;
import com.laneflow.engine.modules.workflow.model.enums.WorkflowVersionStatus;
import com.laneflow.engine.modules.workflow.repository.WorkflowDefinitionRepository;
import com.laneflow.engine.modules.workflow.repository.WorkflowVersionRepository;
import com.laneflow.engine.modules.workflow.request.CreateWorkflowRequest;
import com.laneflow.engine.modules.workflow.request.UpdateWorkflowRequest;
import com.laneflow.engine.modules.workflow.response.WorkflowResponse;
import com.laneflow.engine.modules.workflow.response.WorkflowSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final RepositoryService repositoryService;
    private final BpmnMetadataExtractor bpmnMetadataExtractor;
    private final WorkflowModelValidator workflowModelValidator;
    private final WorkflowAuditService workflowAuditService;
    private final DynamicFormService dynamicFormService;
    private final WorkflowAccessService workflowAccessService;

    @Override
    public List<WorkflowSummaryResponse> findAll(String username) {
        return workflowAccessService.filterReadable(workflowDefinitionRepository.findAll(), username)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Override
    public WorkflowResponse findById(String id, String username) {
        WorkflowDefinition wf = workflowAccessService.requireReadable(id, username);
        return toResponse(wf);
    }

    @Override
    public WorkflowResponse create(CreateWorkflowRequest request, String createdBy) {
        if (workflowDefinitionRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Ya existe un workflow con el codigo: " + request.code());
        }

        List<WorkflowNode> nodes = request.nodes() != null
                ? request.nodes().stream().map(this::mapNode).toList()
                : List.of();
        List<WorkflowTransition> transitions = request.transitions() != null
                ? request.transitions().stream().map(this::mapTransition).toList()
                : List.of();
        List<Swimlane> swimlanes = request.swimlanes() != null
                ? request.swimlanes().stream().map(this::mapSwimlane).toList()
                : List.of();

        if (request.bpmnXml() != null && !request.bpmnXml().isBlank()) {
            BpmnMetadataExtractor.BpmnStructure structure = bpmnMetadataExtractor.extract(request.bpmnXml());
            swimlanes = structure.swimlanes();
            nodes = structure.nodes();
            transitions = structure.transitions();
        }

        workflowModelValidator.validateDraft(request.bpmnXml(), swimlanes, nodes, transitions);

        WorkflowDefinition wf = WorkflowDefinition.builder()
                .code(request.code().toUpperCase())
                .name(request.name())
                .description(request.description())
                .status(WorkflowStatus.DRAFT)
                .currentVersion(1)
                .camundaProcessKey("process_" + request.code().toLowerCase())
                .draftBpmnXml(normalizeXml(request.bpmnXml()))
                .publishedVersionNumber(null)
                .swimlanes(swimlanes)
                .nodes(nodes)
                .transitions(transitions)
                .createdBy(createdBy)
                .lastModifiedBy(createdBy)
                .build();

        WorkflowDefinition saved = workflowDefinitionRepository.save(wf);
        dynamicFormService.syncNodeBindings(saved.getId(), saved.getNodes());
        saved = workflowDefinitionRepository.save(saved);
        workflowAuditService.record(
                saved,
                WorkflowAuditAction.WORKFLOW_CREATED,
                "Creacion inicial de la politica.",
                createdBy,
                null,
                saved.getStatus(),
                Map.of(
                        "workflowCode", saved.getCode(),
                        "workflowName", saved.getName()
                )
        );
        log.info("Workflow creado: {} por {}", saved.getCode(), createdBy);
        return toResponse(saved);
    }

    @Override
    public WorkflowResponse update(String id, UpdateWorkflowRequest request, String updatedBy) {
        WorkflowDefinition wf = workflowAccessService.requireWritable(id, updatedBy);

        if (wf.getStatus() != WorkflowStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden editar workflows en estado DRAFT.");
        }

        WorkflowStatus statusBefore = wf.getStatus();

        if (request.name() != null) {
            wf.setName(request.name());
        }
        if (request.description() != null) {
            wf.setDescription(request.description());
        }
        if (request.bpmnXml() != null) {
            wf.setDraftBpmnXml(normalizeXml(request.bpmnXml()));
            if (wf.getDraftBpmnXml() != null) {
                BpmnMetadataExtractor.BpmnStructure structure = bpmnMetadataExtractor.extract(wf.getDraftBpmnXml());
                workflowModelValidator.validateDraft(wf.getDraftBpmnXml(), structure.swimlanes(), structure.nodes(), structure.transitions());
                dynamicFormService.validateNodeBindings(wf.getId(), structure.nodes());
                wf.setSwimlanes(structure.swimlanes());
                wf.setNodes(structure.nodes());
                wf.setTransitions(structure.transitions());
                dynamicFormService.syncNodeBindings(wf.getId(), wf.getNodes());
            }
        }

        if (request.swimlanes() != null) {
            wf.setSwimlanes(request.swimlanes().stream().map(s -> Swimlane.builder()
                    .id(s.id())
                    .name(s.name())
                    .departmentId(s.departmentId())
                    .departmentCode(s.departmentCode())
                    .build()).toList());
        }

        if (request.nodes() != null) {
            List<WorkflowNode> nodes = request.nodes().stream().map(n -> WorkflowNode.builder()
                    .id(n.id())
                    .name(n.name())
                    .type(n.type())
                    .swimlaneId(n.swimlaneId())
                    .departmentId(n.departmentId())
                    .formKey(n.formKey())
                    .requiredAction(n.requiredAction())
                    .build()).toList();
            wf.setNodes(nodes);
        }

        if (request.transitions() != null) {
            List<WorkflowTransition> transitions = request.transitions().stream().map(t -> WorkflowTransition.builder()
                    .id(t.id())
                    .sourceNodeId(t.sourceNodeId())
                    .targetNodeId(t.targetNodeId())
                    .condition(t.condition())
                    .label(t.label())
                    .build()).toList();
            wf.setTransitions(transitions);
        }

        workflowModelValidator.validateDraft(wf.getDraftBpmnXml(), wf.getSwimlanes(), wf.getNodes(), wf.getTransitions());
        dynamicFormService.validateNodeBindings(wf.getId(), wf.getNodes());

        wf.setLastModifiedBy(updatedBy);
        wf.setUpdatedAt(LocalDateTime.now());
        dynamicFormService.syncNodeBindings(wf.getId(), wf.getNodes());
        WorkflowDefinition saved = workflowDefinitionRepository.save(wf);
        workflowAuditService.record(
                saved,
                WorkflowAuditAction.WORKFLOW_UPDATED,
                "Actualizacion del borrador de la politica.",
                updatedBy,
                statusBefore,
                saved.getStatus(),
                Map.of(
                        "workflowCode", saved.getCode(),
                        "workflowName", saved.getName(),
                        "hasBpmnXml", saved.getDraftBpmnXml() != null
                )
        );
        return toResponse(saved);
    }

    @Override
    public WorkflowResponse publish(String id, String publishedBy) {
        WorkflowDefinition wf = workflowAccessService.requireWritable(id, publishedBy);
        WorkflowStatus statusBefore = wf.getStatus();

        String bpmnXml = BpmnDeploymentPreparer.prepareForDeployment(
                resolveBpmnXmlForPublish(wf),
                wf.getCamundaProcessKey(),
                wf.getName()
        );
        BpmnMetadataExtractor.BpmnStructure publishStructure = bpmnMetadataExtractor.extract(bpmnXml);
        workflowModelValidator.validatePublishable(
                wf.getCode(),
                wf.getName(),
                publishStructure.swimlanes(),
                publishStructure.nodes(),
                publishStructure.transitions(),
                publishStructure.participantCount(),
                publishStructure.laneCount()
        );
        wf.setSwimlanes(publishStructure.swimlanes());
        wf.setNodes(publishStructure.nodes());
        wf.setTransitions(publishStructure.transitions());
        dynamicFormService.syncNodeBindings(wf.getId(), wf.getNodes());
        dynamicFormService.validateNodeBindings(wf.getId(), wf.getNodes());
        int nextVersionNumber = resolveNextVersionNumber(wf.getId());

        try {
            workflowVersionRepository.findByWorkflowDefinitionIdAndStatus(wf.getId(), WorkflowVersionStatus.PUBLISHED)
                    .ifPresent(prev -> {
                        prev.setStatus(WorkflowVersionStatus.DEPRECATED);
                        workflowVersionRepository.save(prev);
                    });

            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(wf.getCamundaProcessKey() + "_v" + nextVersionNumber + ".bpmn",
                            new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)))
                    .name(wf.getName() + " v" + nextVersionNumber)
                    .deploy();

            String processDefinitionId = CamundaDeploymentSupport
                    .resolveProcessDefinition(repositoryService, deployment, wf.getCamundaProcessKey())
                    .getId();

            wf.setCamundaDeploymentId(deployment.getId());
            wf.setCamundaProcessDefinitionId(processDefinitionId);

            log.info("Workflow {} desplegado en Camunda. DeploymentId: {}", wf.getCode(), deployment.getId());

            Map<String, Object> snapshot = Map.of(
                    "code", wf.getCode(),
                    "name", wf.getName(),
                    "nodes", wf.getNodes(),
                    "transitions", wf.getTransitions(),
                    "swimlanes", wf.getSwimlanes()
            );

            WorkflowVersion version = WorkflowVersion.builder()
                    .workflowDefinitionId(wf.getId())
                    .versionNumber(nextVersionNumber)
                    .bpmnXml(bpmnXml)
                    .status(WorkflowVersionStatus.PUBLISHED)
                    .camundaDeploymentId(deployment.getId())
                    .camundaProcessDefinitionId(processDefinitionId)
                    .snapshot(snapshot)
                    .createdBy(publishedBy)
                    .publishedAt(LocalDateTime.now())
                    .build();

            workflowVersionRepository.save(version);
        } catch (Exception e) {
            log.error("Error al desplegar workflow {} en Camunda: {}", wf.getCode(), e.getMessage(), e);
            throw new IllegalStateException("Error al desplegar el workflow en Camunda: " + e.getMessage(), e);
        }

        wf.setStatus(WorkflowStatus.PUBLISHED);
        wf.setCurrentVersion(nextVersionNumber);
        wf.setPublishedVersionNumber(nextVersionNumber);
        wf.setDraftBpmnXml(bpmnXml);
        wf.setLastModifiedBy(publishedBy);
        wf.setPublishedAt(LocalDateTime.now());
        wf.setUpdatedAt(LocalDateTime.now());
        WorkflowDefinition saved = workflowDefinitionRepository.save(wf);
        workflowAuditService.record(
                saved,
                WorkflowAuditAction.WORKFLOW_PUBLISHED,
                "Publicacion de la politica en Camunda.",
                publishedBy,
                statusBefore,
                saved.getStatus(),
                Map.of(
                        "workflowCode", saved.getCode(),
                        "workflowName", saved.getName(),
                        "versionNumber", nextVersionNumber,
                        "camundaDeploymentId", saved.getCamundaDeploymentId(),
                        "camundaProcessDefinitionId", saved.getCamundaProcessDefinitionId()
                )
        );

        return toResponse(saved);
    }

    @Override
    public void delete(String id, String deletedBy) {
        WorkflowDefinition wf = workflowAccessService.requireWritable(id, deletedBy);

        if (wf.getStatus() != WorkflowStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden eliminar workflows en estado DRAFT.");
        }

        workflowAuditService.record(
                wf,
                WorkflowAuditAction.WORKFLOW_DELETED,
                "Eliminacion de la politica en borrador.",
                deletedBy,
                wf.getStatus(),
                null,
                Map.of(
                        "workflowCode", wf.getCode(),
                        "workflowName", wf.getName()
                )
        );
        workflowDefinitionRepository.delete(wf);
        log.info("Workflow eliminado: {}", wf.getCode());
    }

    @Override
    public WorkflowResponse validate(String id, String username) {
        WorkflowDefinition wf = workflowAccessService.requireReadable(id, username);
        workflowModelValidator.validateCompleteDraft(wf.getSwimlanes(), wf.getNodes(), wf.getTransitions());
        return toResponse(wf);
    }

    private String resolveBpmnXmlForPublish(WorkflowDefinition wf) {
        if (wf.getDraftBpmnXml() != null && !wf.getDraftBpmnXml().isBlank()) {
            return wf.getDraftBpmnXml();
        }

        workflowModelValidator.validatePublishable(wf);
        return generateBpmnXml(wf);
    }

    private int resolveNextVersionNumber(String workflowId) {
        return workflowVersionRepository.findByWorkflowDefinitionIdOrderByVersionNumberDesc(workflowId)
                .stream()
                .findFirst()
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
    }

    private String generateBpmnXml(WorkflowDefinition wf) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n");
        sb.append("             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("             xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\"\n");
        sb.append("             targetNamespace=\"http://camunda.org/schema/1.0/bpmn\">\n");
        sb.append("  <process id=\"").append(wf.getCamundaProcessKey()).append("\"");
        sb.append(" name=\"").append(escapeXml(wf.getName())).append("\"");
        sb.append(" isExecutable=\"true\"");
        sb.append(" camunda:historyTimeToLive=\"180\">\n");

        for (WorkflowNode node : wf.getNodes()) {
            String id = node.getId();
            String name = escapeXml(node.getName());
            switch (node.getType()) {
                case START_EVENT ->
                        sb.append("    <startEvent id=\"").append(id).append("\" name=\"").append(name).append("\"/>\n");
                case END_EVENT ->
                        sb.append("    <endEvent id=\"").append(id).append("\" name=\"").append(name).append("\"/>\n");
                case INTERMEDIATE_EVENT ->
                        sb.append("    <intermediateCatchEvent id=\"").append(id).append("\" name=\"").append(name).append("\"/>\n");
                case USER_TASK -> {
                    sb.append("    <userTask id=\"").append(id).append("\" name=\"").append(name).append("\"");
                    if (node.getDepartmentId() != null && !node.getDepartmentId().isBlank()) {
                        sb.append(" camunda:candidateGroups=\"").append(node.getDepartmentId()).append("\"");
                    }
                    sb.append("/>\n");
                }
                case EXCLUSIVE_GATEWAY ->
                        sb.append("    <exclusiveGateway id=\"").append(id).append("\" name=\"").append(name).append("\"/>\n");
                case PARALLEL_GATEWAY ->
                        sb.append("    <parallelGateway id=\"").append(id).append("\" name=\"").append(name).append("\"/>\n");
                case INCLUSIVE_GATEWAY ->
                        sb.append("    <inclusiveGateway id=\"").append(id).append("\" name=\"").append(name).append("\"/>\n");
                default ->
                        sb.append("    <task id=\"").append(id).append("\" name=\"").append(name).append("\"/>\n");
            }
        }

        for (WorkflowTransition transition : wf.getTransitions()) {
            sb.append("    <sequenceFlow id=\"").append(transition.getId())
                    .append("\" sourceRef=\"").append(transition.getSourceNodeId())
                    .append("\" targetRef=\"").append(transition.getTargetNodeId()).append("\"");
            if (transition.getCondition() != null && !transition.getCondition().isBlank()) {
                sb.append(">\n      <conditionExpression xsi:type=\"tFormalExpression\">")
                        .append(escapeXml(transition.getCondition()))
                        .append("</conditionExpression>\n    </sequenceFlow>\n");
            } else {
                sb.append("/>\n");
            }
        }

        sb.append("  </process>\n");
        sb.append("</definitions>\n");
        return sb.toString();
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String normalizeXml(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Swimlane mapSwimlane(CreateWorkflowRequest.SwimlaneRequest swimlane) {
        return Swimlane.builder()
                .id(swimlane.id())
                .name(swimlane.name())
                .departmentId(swimlane.departmentId())
                .departmentCode(swimlane.departmentCode())
                .build();
    }

    private WorkflowNode mapNode(CreateWorkflowRequest.NodeRequest node) {
        return WorkflowNode.builder()
                .id(node.id())
                .name(node.name())
                .type(node.type())
                .swimlaneId(node.swimlaneId())
                .departmentId(node.departmentId())
                .formKey(node.formKey())
                .requiredAction(node.requiredAction())
                .build();
    }

    private WorkflowTransition mapTransition(CreateWorkflowRequest.TransitionRequest transition) {
        return WorkflowTransition.builder()
                .id(transition.id())
                .sourceNodeId(transition.sourceNodeId())
                .targetNodeId(transition.targetNodeId())
                .condition(transition.condition())
                .label(transition.label())
                .build();
    }

    private WorkflowSummaryResponse toSummaryResponse(WorkflowDefinition wf) {
        return new WorkflowSummaryResponse(
                wf.getId(),
                wf.getCode(),
                wf.getName(),
                wf.getDescription(),
                wf.getStatus(),
                wf.getCurrentVersion(),
                wf.getPublishedVersionNumber(),
                wf.getCreatedAt(),
                wf.getUpdatedAt()
        );
    }

    private WorkflowResponse toResponse(WorkflowDefinition wf) {
        return new WorkflowResponse(
                wf.getId(),
                wf.getCode(),
                wf.getName(),
                wf.getDescription(),
                wf.getStatus(),
                wf.getCurrentVersion(),
                wf.getCamundaProcessKey(),
                wf.getDraftBpmnXml(),
                wf.getPublishedVersionNumber(),
                wf.getSwimlanes(),
                wf.getNodes(),
                wf.getTransitions(),
                wf.getCreatedBy(),
                wf.getLastModifiedBy(),
                wf.getCreatedAt(),
                wf.getUpdatedAt(),
                wf.getPublishedAt()
        );
    }
}
