package com.laneflow.engine.modules.workflow.service;

import com.laneflow.engine.modules.admin.model.Department;
import com.laneflow.engine.modules.admin.repository.DepartmentRepository;
import com.laneflow.engine.modules.workflow.model.embedded.Swimlane;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowNode;
import com.laneflow.engine.modules.workflow.model.embedded.WorkflowTransition;
import com.laneflow.engine.modules.workflow.model.enums.NodeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class BpmnMetadataExtractor {

    private static final String CAMUNDA_NAMESPACE = "http://camunda.org/schema/1.0/bpmn";

    private final DepartmentRepository departmentRepository;

    BpmnStructure extract(String bpmnXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(bpmnXml)));

            Map<String, DepartmentRef> departmentsByNormalizedKey = loadDepartments();
            Map<String, Swimlane> swimlanesById = new LinkedHashMap<>();
            Map<String, String> nodeLaneMap = new HashMap<>();
            Map<String, String> processSwimlaneMap = new HashMap<>();
            Map<String, String> processNamesById = loadProcessNames(document);

            NodeList laneNodes = document.getElementsByTagNameNS("*", "lane");
            for (int i = 0; i < laneNodes.getLength(); i++) {
                Element lane = (Element) laneNodes.item(i);
                String laneId = lane.getAttribute("id");
                String laneName = firstNonBlank(lane.getAttribute("name"), laneId);
                DepartmentRef department = resolveDepartment(laneName, departmentsByNormalizedKey);

                swimlanesById.put(laneId, Swimlane.builder()
                        .id(laneId)
                        .name(laneName)
                        .departmentId(department == null ? null : department.id())
                        .departmentCode(department == null ? null : department.code())
                        .build());

                NodeList flowNodeRefs = lane.getElementsByTagNameNS("*", "flowNodeRef");
                for (int j = 0; j < flowNodeRefs.getLength(); j++) {
                    String flowNodeId = trimToNull(flowNodeRefs.item(j).getTextContent());
                    if (flowNodeId != null) {
                        nodeLaneMap.put(flowNodeId, laneId);
                    }
                }
            }

            NodeList participantNodes = document.getElementsByTagNameNS("*", "participant");
            for (int i = 0; i < participantNodes.getLength(); i++) {
                Element participant = (Element) participantNodes.item(i);
                String participantId = trimToNull(participant.getAttribute("id"));
                String processRef = trimToNull(participant.getAttribute("processRef"));
                if (participantId == null || processRef == null) {
                    continue;
                }

                String participantName = firstNonBlank(
                        firstNonBlank(
                                trimToNull(participant.getAttribute("name")),
                                processNamesById.get(processRef)
                        ),
                        processRef
                );
                DepartmentRef department = resolveDepartment(participantName, departmentsByNormalizedKey);

                swimlanesById.putIfAbsent(participantId, Swimlane.builder()
                        .id(participantId)
                        .name(participantName)
                        .departmentId(department == null ? null : department.id())
                        .departmentCode(department == null ? null : department.code())
                        .build());

                processSwimlaneMap.put(processRef, participantId);
            }

            List<WorkflowNode> nodes = new ArrayList<>();
            addNodes(document, "startEvent", NodeType.START_EVENT, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);
            addNodes(document, "endEvent", NodeType.END_EVENT, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);
            addNodes(document, "intermediateCatchEvent", NodeType.INTERMEDIATE_EVENT, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);
            addNodes(document, "intermediateThrowEvent", NodeType.INTERMEDIATE_EVENT, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);
            addNodes(document, "boundaryEvent", NodeType.INTERMEDIATE_EVENT, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);
            addNodes(document, "task", NodeType.USER_TASK, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);
            addNodes(document, "userTask", NodeType.USER_TASK, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);
            addNodes(document, "serviceTask", NodeType.SERVICE_TASK, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);
            addNodes(document, "exclusiveGateway", NodeType.EXCLUSIVE_GATEWAY, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);
            addNodes(document, "parallelGateway", NodeType.PARALLEL_GATEWAY, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);
            addNodes(document, "inclusiveGateway", NodeType.INCLUSIVE_GATEWAY, nodeLaneMap, processSwimlaneMap, swimlanesById, departmentsByNormalizedKey, nodes);

            List<WorkflowTransition> transitions = new ArrayList<>();
            NodeList flowNodes = document.getElementsByTagNameNS("*", "sequenceFlow");
            for (int i = 0; i < flowNodes.getLength(); i++) {
                Element flow = (Element) flowNodes.item(i);
                transitions.add(WorkflowTransition.builder()
                        .id(flow.getAttribute("id"))
                        .sourceNodeId(flow.getAttribute("sourceRef"))
                        .targetNodeId(flow.getAttribute("targetRef"))
                        .label(trimToNull(flow.getAttribute("name")))
                        .condition(readConditionExpression(flow))
                        .build());
            }

            return new BpmnStructure(new ArrayList<>(swimlanesById.values()), nodes, transitions);
        } catch (Exception e) {
            throw new IllegalArgumentException("El BPMN XML no es válido: " + e.getMessage(), e);
        }
    }

    private void addNodes(
            Document document,
            String tagName,
            NodeType nodeType,
            Map<String, String> nodeLaneMap,
            Map<String, String> processSwimlaneMap,
            Map<String, Swimlane> swimlanesById,
            Map<String, DepartmentRef> departmentsByNormalizedKey,
            List<WorkflowNode> target
    ) {
        NodeList xmlNodes = document.getElementsByTagNameNS("*", tagName);
        for (int i = 0; i < xmlNodes.getLength(); i++) {
            Element element = (Element) xmlNodes.item(i);
            String nodeId = element.getAttribute("id");
            String swimlaneId = nodeLaneMap.get(nodeId);
            if (swimlaneId == null) {
                String processId = resolveOwningProcessId(element);
                if (processId != null) {
                    swimlaneId = processSwimlaneMap.get(processId);
                }
            }
            String candidateGroups = firstNonBlank(
                    trimToNull(element.getAttributeNS(CAMUNDA_NAMESPACE, "candidateGroups")),
                    trimToNull(element.getAttribute("camunda:candidateGroups"))
            );
            DepartmentRef department = resolveDepartment(candidateGroups, departmentsByNormalizedKey);

            if (department == null && swimlaneId != null) {
                Swimlane swimlane = swimlanesById.get(swimlaneId);
                if (swimlane != null && swimlane.getDepartmentId() != null) {
                    department = new DepartmentRef(swimlane.getDepartmentId(), swimlane.getDepartmentCode());
                }
            }

            target.add(WorkflowNode.builder()
                    .id(nodeId)
                    .name(firstNonBlank(trimToNull(element.getAttribute("name")), nodeId))
                    .type(nodeType)
                    .swimlaneId(swimlaneId)
                    .departmentId(department == null ? null : department.id())
                    .formKey(firstNonBlank(
                            trimToNull(element.getAttributeNS(CAMUNDA_NAMESPACE, "formKey")),
                            trimToNull(element.getAttribute("camunda:formKey"))
                    ))
                    .requiredAction(null)
                    .build());
        }
    }

    private String readConditionExpression(Element sequenceFlow) {
        NodeList children = sequenceFlow.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element && "conditionExpression".equals(element.getLocalName())) {
                return trimToNull(element.getTextContent());
            }
        }
        return null;
    }

    private String resolveOwningProcessId(Element element) {
        Node current = element.getParentNode();
        while (current != null) {
            if (current instanceof Element parent && "process".equals(parent.getLocalName())) {
                return trimToNull(parent.getAttribute("id"));
            }
            current = current.getParentNode();
        }
        return null;
    }

    private Map<String, DepartmentRef> loadDepartments() {
        Map<String, DepartmentRef> lookup = new HashMap<>();
        for (Department department : departmentRepository.findByActiveTrueOrderByCodeAsc()) {
            DepartmentRef ref = new DepartmentRef(department.getId(), department.getCode());
            lookup.put(normalize(department.getCode()), ref);
            lookup.put(normalize(department.getName()), ref);
        }
        return lookup;
    }

    private Map<String, String> loadProcessNames(Document document) {
        Map<String, String> processNames = new HashMap<>();
        NodeList processNodes = document.getElementsByTagNameNS("*", "process");
        for (int i = 0; i < processNodes.getLength(); i++) {
            Element process = (Element) processNodes.item(i);
            String processId = trimToNull(process.getAttribute("id"));
            if (processId == null) {
                continue;
            }
            processNames.put(processId, trimToNull(process.getAttribute("name")));
        }
        return processNames;
    }

    private DepartmentRef resolveDepartment(String rawValue, Map<String, DepartmentRef> departmentsByNormalizedKey) {
        String value = trimToNull(rawValue);
        if (value == null) {
            return null;
        }

        for (String token : value.split(",")) {
            DepartmentRef ref = departmentsByNormalizedKey.get(normalize(token));
            if (ref != null) {
                return ref;
            }
        }

        return null;
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    record BpmnStructure(
            List<Swimlane> swimlanes,
            List<WorkflowNode> nodes,
            List<WorkflowTransition> transitions
    ) {}

    private record DepartmentRef(String id, String code) {}
}
