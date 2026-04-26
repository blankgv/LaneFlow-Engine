package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.workflow.model.WorkflowDefinition;
import com.laneflow.engine.modules.workflow.model.embedded.Swimlane;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowTransition;
import com.laneflow.engine.modules.workflow.model.enums.NodeType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class WorkflowModelValidator {

    void validateDraft(String bpmnXml, List<Swimlane> swimlanes, List<WorkflowNode> nodes, List<WorkflowTransition> transitions) {
        if (bpmnXml != null && !bpmnXml.isBlank()) {
            validateStructure(swimlanes, nodes, transitions);
            return;
        }

        validateStructure(swimlanes, nodes, transitions);
    }

    void validatePublishable(WorkflowDefinition workflow) {
        validatePublishable(workflow.getCode(), workflow.getName(), workflow.getSwimlanes(), workflow.getNodes(), workflow.getTransitions());
    }

    void validatePublishable(
            String workflowCode,
            String workflowName,
            List<Swimlane> swimlanes,
            List<WorkflowNode> nodes,
            List<WorkflowTransition> transitions
    ) {
        validateStructure(swimlanes, nodes, transitions);

        List<WorkflowNode> userTasks = nodes.stream()
                .filter(node -> node.getType() == NodeType.USER_TASK)
                .toList();

        if (userTasks.isEmpty()) {
            throw new IllegalArgumentException("La politica debe tener al menos una User Task antes de publicarse.");
        }

        if (swimlanes == null || swimlanes.isEmpty()) {
            throw new IllegalArgumentException("La politica debe tener lanes que representen departamentos.");
        }

        Map<String, Swimlane> swimlanesById = swimlanes.stream()
                .collect(Collectors.toMap(Swimlane::getId, Function.identity(), (left, right) -> left));

        for (WorkflowNode userTask : userTasks) {
            if (userTask.getSwimlaneId() == null || userTask.getSwimlaneId().isBlank()) {
                throw new IllegalArgumentException("La tarea '" + userTask.getName() + "' debe pertenecer a una lane/departamento.");
            }

            Swimlane swimlane = swimlanesById.get(userTask.getSwimlaneId());
            if (swimlane == null) {
                throw new IllegalArgumentException("La tarea '" + userTask.getName() + "' referencia una lane inexistente.");
            }

            if (swimlane.getDepartmentId() == null || swimlane.getDepartmentId().isBlank()) {
                throw new IllegalArgumentException(
                        "La lane '" + swimlane.getName() + "' no esta vinculada a un departamento valido. " +
                        "Usa el nombre o codigo exacto del departamento."
                );
            }
        }
    }

    private void validateStructure(List<Swimlane> swimlanes, List<WorkflowNode> nodes, List<WorkflowTransition> transitions) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("El flujo debe tener al menos un nodo.");
        }

        ensureUniqueNodeIds(nodes);

        boolean hasStart = nodes.stream().anyMatch(n -> n.getType() == NodeType.START_EVENT);
        boolean hasEnd = nodes.stream().anyMatch(n -> n.getType() == NodeType.END_EVENT);

        if (!hasStart) {
            throw new IllegalArgumentException("El flujo debe tener al menos un nodo de inicio (START_EVENT).");
        }
        if (!hasEnd) {
            throw new IllegalArgumentException("El flujo debe tener al menos un nodo de fin (END_EVENT).");
        }

        Map<String, WorkflowNode> nodesById = nodes.stream()
                .collect(Collectors.toMap(WorkflowNode::getId, Function.identity(), (left, right) -> left));
        Set<String> swimlaneIds = swimlanes == null
                ? Set.of()
                : swimlanes.stream().map(Swimlane::getId).collect(Collectors.toSet());

        for (WorkflowNode node : nodes) {
            if (node.getId() == null || node.getId().isBlank()) {
                throw new IllegalArgumentException("Todos los nodos del flujo deben tener id.");
            }

            if (node.getType() == NodeType.USER_TASK && node.getSwimlaneId() != null && !node.getSwimlaneId().isBlank()
                    && !swimlaneIds.contains(node.getSwimlaneId())) {
                throw new IllegalArgumentException("La tarea '" + node.getName() + "' referencia una lane inexistente.");
            }
        }

        if (transitions == null) {
            return;
        }

        for (WorkflowTransition transition : transitions) {
            if (transition.getId() == null || transition.getId().isBlank()) {
                throw new IllegalArgumentException("Todas las transiciones del flujo deben tener id.");
            }

            if (!nodesById.containsKey(transition.getSourceNodeId())) {
                throw new IllegalArgumentException("Transicion con nodo fuente invalido: " + transition.getSourceNodeId());
            }
            if (!nodesById.containsKey(transition.getTargetNodeId())) {
                throw new IllegalArgumentException("Transicion con nodo destino invalido: " + transition.getTargetNodeId());
            }
        }
    }

    private void ensureUniqueNodeIds(List<WorkflowNode> nodes) {
        long uniqueIds = nodes.stream()
                .map(WorkflowNode::getId)
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .count();

        if (uniqueIds != nodes.size()) {
            throw new IllegalArgumentException("El flujo contiene nodos con id duplicado.");
        }
    }
}
