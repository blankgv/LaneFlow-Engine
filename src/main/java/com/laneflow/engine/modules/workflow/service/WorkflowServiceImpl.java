package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.WorkflowVersion;
import com.laneflow.engine.modules.workflow.model.embedded.Swimlane;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowTransition;
import com.laneflow.engine.modules.workflow.model.enums.NodeType;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowDefinitionRepository workflowDefinitionRepository;
    private final WorkflowVersionRepository workflowVersionRepository;
    private final RepositoryService repositoryService;

    @Override
    public List<WorkflowSummaryResponse> findAll() {
        return workflowDefinitionRepository.findAll()
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Override
    public WorkflowResponse findById(String id) {
        WorkflowDefinition wf = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + id));
        return toResponse(wf);
    }

    @Override
    public WorkflowResponse create(CreateWorkflowRequest request, String createdBy) {
        if (workflowDefinitionRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Ya existe un workflow con el código: " + request.code());
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

        validateDraftPayload(request.bpmnXml(), nodes, transitions);

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
        log.info("Workflow creado: {} por {}", saved.getCode(), createdBy);
        return toResponse(saved);
    }

    @Override
    public WorkflowResponse update(String id, UpdateWorkflowRequest request, String updatedBy) {
        WorkflowDefinition wf = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + id));

        if (wf.getStatus() != WorkflowStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden editar workflows en estado DRAFT.");
        }

        if (request.name() != null) wf.setName(request.name());
        if (request.description() != null) wf.setDescription(request.description());
        if (request.bpmnXml() != null) wf.setDraftBpmnXml(normalizeXml(request.bpmnXml()));

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

        validateDraftPayload(wf.getDraftBpmnXml(), wf.getNodes(), wf.getTransitions());

        wf.setLastModifiedBy(updatedBy);
        wf.setUpdatedAt(LocalDateTime.now());
        return toResponse(workflowDefinitionRepository.save(wf));
    }

    @Override
    public WorkflowResponse publish(String id, String publishedBy) {
        WorkflowDefinition wf = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + id));

        validateStructure(wf.getNodes(), wf.getTransitions());

        String bpmnXml = generateBpmnXml(wf);

        try {
            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(wf.getCamundaProcessKey() + ".bpmn",
                            new ByteArrayInputStream(bpmnXml.getBytes(StandardCharsets.UTF_8)))
                    .name(wf.getName())
                    .deploy();

            String processDefinitionId = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .singleResult()
                    .getId();

            wf.setCamundaDeploymentId(deployment.getId());
            wf.setCamundaProcessDefinitionId(processDefinitionId);

            log.info("Workflow {} desplegado en Camunda. DeploymentId: {}", wf.getCode(), deployment.getId());

            // Save version snapshot
            Map<String, Object> snapshot = Map.of(
                    "code", wf.getCode(),
                    "name", wf.getName(),
                    "nodes", wf.getNodes(),
                    "transitions", wf.getTransitions(),
                    "swimlanes", wf.getSwimlanes()
            );

            WorkflowVersion version = WorkflowVersion.builder()
                    .workflowDefinitionId(wf.getId())
                    .versionNumber(wf.getCurrentVersion())
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
            log.error("Error al desplegar workflow {} en Camunda: {}", wf.getCode(), e.getMessage());
            throw new IllegalStateException("Error al desplegar el workflow en Camunda: " + e.getMessage(), e);
        }

        wf.setStatus(WorkflowStatus.PUBLISHED);
        wf.setPublishedVersionNumber(wf.getCurrentVersion());
        wf.setLastModifiedBy(publishedBy);
        wf.setPublishedAt(LocalDateTime.now());
        wf.setUpdatedAt(LocalDateTime.now());

        return toResponse(workflowDefinitionRepository.save(wf));
    }

    @Override
    public void delete(String id) {
        WorkflowDefinition wf = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + id));

        if (wf.getStatus() != WorkflowStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden eliminar workflows en estado DRAFT.");
        }

        workflowDefinitionRepository.delete(wf);
        log.info("Workflow eliminado: {}", wf.getCode());
    }

    @Override
    public WorkflowResponse validate(String id) {
        WorkflowDefinition wf = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + id));

        validateDraftPayload(wf.getDraftBpmnXml(), wf.getNodes(), wf.getTransitions());
        return toResponse(wf);
    }

    // ------------------------------------------------------------------ helpers

    private void validateDraftPayload(String bpmnXml, List<WorkflowNode> nodes, List<WorkflowTransition> transitions) {
        if (bpmnXml != null && !bpmnXml.isBlank()) {
            return;
        }

        validateStructure(nodes, transitions);
    }

    private void validateStructure(List<WorkflowNode> nodes, List<WorkflowTransition> transitions) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("El flujo debe tener al menos un nodo.");
        }

        boolean hasStart = nodes.stream().anyMatch(n -> n.getType() == NodeType.START_EVENT);
        boolean hasEnd = nodes.stream().anyMatch(n -> n.getType() == NodeType.END_EVENT);

        if (!hasStart) throw new IllegalArgumentException("El flujo debe tener al menos un nodo de inicio (START_EVENT).");
        if (!hasEnd) throw new IllegalArgumentException("El flujo debe tener al menos un nodo de fin (END_EVENT).");

        if (transitions != null) {
            Set<String> nodeIds = nodes.stream().map(WorkflowNode::getId).collect(Collectors.toSet());
            for (WorkflowTransition t : transitions) {
                if (!nodeIds.contains(t.getSourceNodeId()))
                    throw new IllegalArgumentException("Transición con nodo fuente inválido: " + t.getSourceNodeId());
                if (!nodeIds.contains(t.getTargetNodeId()))
                    throw new IllegalArgumentException("Transición con nodo destino inválido: " + t.getTargetNodeId());
            }
        }
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
        sb.append(" camunda:historyTimeToLive=\"P180D\">\n");

        for (WorkflowNode node : wf.getNodes()) {
            String id = node.getId();
            String name = escapeXml(node.getName());
            switch (node.getType()) {
                case START_EVENT ->
                    sb.append("    <startEvent id=\"").append(id).append("\" name=\"").append(name).append("\"/>\n");
                case END_EVENT ->
                    sb.append("    <endEvent id=\"").append(id).append("\" name=\"").append(name).append("\"/>\n");
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

        for (WorkflowTransition t : wf.getTransitions()) {
            sb.append("    <sequenceFlow id=\"").append(t.getId())
              .append("\" sourceRef=\"").append(t.getSourceNodeId())
              .append("\" targetRef=\"").append(t.getTargetNodeId()).append("\"");
            if (t.getCondition() != null && !t.getCondition().isBlank()) {
                sb.append(">\n      <conditionExpression xsi:type=\"tFormalExpression\">")
                  .append(escapeXml(t.getCondition()))
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
        if (value == null) return "";
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

    private Swimlane mapSwimlane(CreateWorkflowRequest.SwimlaneRequest s) {
        return Swimlane.builder()
                .id(s.id())
                .name(s.name())
                .departmentId(s.departmentId())
                .departmentCode(s.departmentCode())
                .build();
    }

    private WorkflowNode mapNode(CreateWorkflowRequest.NodeRequest n) {
        return WorkflowNode.builder()
                .id(n.id())
                .name(n.name())
                .type(n.type())
                .swimlaneId(n.swimlaneId())
                .departmentId(n.departmentId())
                .formKey(n.formKey())
                .requiredAction(n.requiredAction())
                .build();
    }

    private WorkflowTransition mapTransition(CreateWorkflowRequest.TransitionRequest t) {
        return WorkflowTransition.builder()
                .id(t.id())
                .sourceNodeId(t.sourceNodeId())
                .targetNodeId(t.targetNodeId())
                .condition(t.condition())
                .label(t.label())
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
