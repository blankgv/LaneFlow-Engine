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

        validateStructure(nodes, transitions);

        WorkflowDefinition wf = WorkflowDefinition.builder()
                .code(request.code().toUpperCase())
                .name(request.name())
                .description(request.description())
                .status(WorkflowStatus.DRAFT)
                .currentVersion(1)
                .camundaProcessKey("process_" + request.code().toLowerCase())
                .swimlanes(swimlanes)
                .nodes(nodes)
                .transitions(transitions)
                .createdBy(createdBy)
                .build();

        WorkflowDefinition saved = workflowDefinitionRepository.save(wf);
        log.info("Workflow creado: {} por {}", saved.getCode(), createdBy);
        return toResponse(saved);
    }

    @Override
    public WorkflowResponse update(String id, UpdateWorkflowRequest request) {
        WorkflowDefinition wf = workflowDefinitionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow no encontrado: " + id));

        if (wf.getStatus() != WorkflowStatus.DRAFT) {
            throw new IllegalStateException("Solo se pueden editar workflows en estado DRAFT.");
        }

        if (request.name() != null) wf.setName(request.name());
        if (request.description() != null) wf.setDescription(request.description());

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

        if (wf.getNodes() != null && wf.getTransitions() != null) {
            validateStructure(wf.getNodes(), wf.getTransitions());
        }

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

        validateStructure(wf.getNodes(), wf.getTransitions());
        return toResponse(wf);
    }

    // ------------------------------------------------------------------ helpers

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
        sb.append("<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" ");
        sb.append("xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" ");
        sb.append("xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" ");
        sb.append("xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" ");
        sb.append("id=\"Definitions_1\" targetNamespace=\"http://bpmn.io/schema/bpmn\">\n");
        sb.append("  <bpmn:process id=\"").append(wf.getCamundaProcessKey()).append("\" ");
        sb.append("name=\"").append(wf.getName()).append("\" isExecutable=\"true\">\n");

        for (WorkflowNode node : wf.getNodes()) {
            switch (node.getType()) {
                case START_EVENT -> sb.append("    <bpmn:startEvent id=\"").append(node.getId()).append("\" name=\"").append(node.getName()).append("\"/>\n");
                case END_EVENT -> sb.append("    <bpmn:endEvent id=\"").append(node.getId()).append("\" name=\"").append(node.getName()).append("\"/>\n");
                case USER_TASK -> sb.append("    <bpmn:userTask id=\"").append(node.getId()).append("\" name=\"").append(node.getName()).append("\" camunda:candidateGroups=\"").append(node.getDepartmentId() != null ? node.getDepartmentId() : "").append("\"/>\n");
                case EXCLUSIVE_GATEWAY -> sb.append("    <bpmn:exclusiveGateway id=\"").append(node.getId()).append("\" name=\"").append(node.getName()).append("\"/>\n");
                case PARALLEL_GATEWAY -> sb.append("    <bpmn:parallelGateway id=\"").append(node.getId()).append("\" name=\"").append(node.getName()).append("\"/>\n");
                default -> sb.append("    <bpmn:task id=\"").append(node.getId()).append("\" name=\"").append(node.getName()).append("\"/>\n");
            }
        }

        for (WorkflowTransition t : wf.getTransitions()) {
            sb.append("    <bpmn:sequenceFlow id=\"").append(t.getId())
                    .append("\" sourceRef=\"").append(t.getSourceNodeId())
                    .append("\" targetRef=\"").append(t.getTargetNodeId()).append("\"");
            if (t.getCondition() != null && !t.getCondition().isBlank()) {
                sb.append(">\n      <bpmn:conditionExpression xsi:type=\"bpmn:tFormalExpression\">")
                        .append(t.getCondition())
                        .append("</bpmn:conditionExpression>\n    </bpmn:sequenceFlow>\n");
            } else {
                sb.append("/>\n");
            }
        }

        sb.append("  </bpmn:process>\n");
        sb.append("</bpmn:definitions>\n");
        return sb.toString();
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
                wf.getSwimlanes(),
                wf.getNodes(),
                wf.getTransitions(),
                wf.getCreatedAt(),
                wf.getUpdatedAt(),
                wf.getPublishedAt()
        );
    }
}
