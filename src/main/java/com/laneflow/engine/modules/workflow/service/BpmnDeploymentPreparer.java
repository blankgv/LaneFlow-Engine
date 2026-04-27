package com.laneflow.engine.modules.workflow.service;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Pattern;

final class BpmnDeploymentPreparer {

    private static final Pattern DEFINITIONS_TAG = Pattern.compile("<([\\w.-]+:)?definitions\\b");
    private static final Pattern PROCESS_TAG = Pattern.compile("<([\\w.-]+:)?process\\b");
    private static final Pattern PROCESS_ID_ATTRIBUTE = Pattern.compile("\\sid\\s*=\\s*\"[^\"]*\"");
    private static final Pattern PROCESS_NAME_ATTRIBUTE = Pattern.compile("\\sname\\s*=\\s*\"[^\"]*\"");
    private static final Pattern PROCESS_EXECUTABLE_ATTRIBUTE = Pattern.compile("\\sisExecutable\\s*=\\s*\"[^\"]*\"");
    private static final Pattern PROCESS_HISTORY_TTL_ATTRIBUTE = Pattern.compile("\\scamunda:historyTimeToLive\\s*=\\s*\"[^\"]*\"");
    private static final Pattern PROCESS_REF_ATTRIBUTE_TEMPLATE = Pattern.compile("\\sprocessRef\\s*=\\s*\"%s\"");

    private BpmnDeploymentPreparer() {
    }

    static String prepareForDeployment(String rawXml, String processKey, String processName) {
        if (rawXml == null || rawXml.isBlank()) {
            throw new IllegalArgumentException("El BPMN XML es obligatorio para publicar.");
        }

        String prepared = rawXml.trim();

        if (!prepared.contains("xmlns:camunda=")) {
            prepared = DEFINITIONS_TAG.matcher(prepared)
                    .replaceFirst(match -> match.group() + " xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\"");
        }

        int processStart = findProcessStart(prepared);
        int processEnd = prepared.indexOf('>', processStart);
        if (processStart < 0 || processEnd < 0) {
            throw new IllegalArgumentException("El BPMN XML no contiene un elemento process válido.");
        }

        String processTag = prepared.substring(processStart, processEnd + 1);
        String originalProcessId = extractAttributeValue(processTag, "id");
        String updatedProcessTag = upsertAttribute(processTag, PROCESS_ID_ATTRIBUTE, "id", processKey);
        updatedProcessTag = upsertAttribute(updatedProcessTag, PROCESS_NAME_ATTRIBUTE, "name", processName);
        updatedProcessTag = upsertAttribute(updatedProcessTag, PROCESS_EXECUTABLE_ATTRIBUTE, "isExecutable", "true");
        updatedProcessTag = upsertAttribute(updatedProcessTag, PROCESS_HISTORY_TTL_ATTRIBUTE, "camunda:historyTimeToLive", "180");

        prepared = prepared.substring(0, processStart)
                + updatedProcessTag
                + prepared.substring(processEnd + 1);

        if (originalProcessId != null && !originalProcessId.equals(processKey)) {
            Pattern participantProcessRefPattern = Pattern.compile(
                    String.format(PROCESS_REF_ATTRIBUTE_TEMPLATE.pattern(), Pattern.quote(originalProcessId))
            );
            prepared = participantProcessRefPattern.matcher(prepared)
                    .replaceFirst(" processRef=\"" + escapeAttribute(processKey) + "\"");
        }

        return prepared;
    }

    static String prepareExecutableDeployment(String preparedXml, String processKey, String processName) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document document = factory.newDocumentBuilder()
                    .parse(new InputSource(new StringReader(preparedXml)));

            Element primaryProcess = resolvePrimaryProcess(document, processKey);
            if (primaryProcess == null) {
                throw new IllegalArgumentException("El BPMN XML no contiene un process ejecutable para Camunda.");
            }

            primaryProcess.setAttribute("id", processKey);
            primaryProcess.setAttribute("name", processName);
            primaryProcess.setAttribute("isExecutable", "true");
            primaryProcess.setAttributeNS("http://camunda.org/schema/1.0/bpmn", "camunda:historyTimeToLive", "180");

            convertGenericTasksToUserTasks(document);
            removeSiblingProcesses(document, primaryProcess);
            removeElements(document, "collaboration");
            removeElements(document, "participant");
            removeElements(document, "messageFlow");
            removeElements(document, "group");
            removeElements(document, "textAnnotation");
            removeDiagramInformation(document);

            return toXml(document);
        } catch (Exception e) {
            throw new IllegalArgumentException("No se pudo preparar el BPMN ejecutable para Camunda: " + e.getMessage(), e);
        }
    }

    private static void convertGenericTasksToUserTasks(Document document) {
        NodeList taskNodes = document.getElementsByTagNameNS("*", "task");
        for (int i = taskNodes.getLength() - 1; i >= 0; i--) {
            Node taskNode = taskNodes.item(i);
            document.renameNode(
                    taskNode,
                    "http://www.omg.org/spec/BPMN/20100524/MODEL",
                    "bpmn:userTask"
            );
        }
    }

    private static Element resolvePrimaryProcess(Document document, String processKey) {
        NodeList processNodes = document.getElementsByTagNameNS("*", "process");
        Element firstProcess = null;
        for (int i = 0; i < processNodes.getLength(); i++) {
            Element process = (Element) processNodes.item(i);
            if (firstProcess == null) {
                firstProcess = process;
            }
            if (processKey.equals(process.getAttribute("id"))) {
                return process;
            }
        }
        return firstProcess;
    }

    private static void removeSiblingProcesses(Document document, Element primaryProcess) {
        NodeList processNodes = document.getElementsByTagNameNS("*", "process");
        for (int i = processNodes.getLength() - 1; i >= 0; i--) {
            Node processNode = processNodes.item(i);
            if (processNode != primaryProcess && processNode.getParentNode() != null) {
                processNode.getParentNode().removeChild(processNode);
            }
        }
    }

    private static void removeElements(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        for (int i = nodes.getLength() - 1; i >= 0; i--) {
            Node node = nodes.item(i);
            if (node.getParentNode() != null) {
                node.getParentNode().removeChild(node);
            }
        }
    }

    private static void removeDiagramInformation(Document document) {
        NodeList diagrams = document.getElementsByTagNameNS("*", "BPMNDiagram");
        for (int i = diagrams.getLength() - 1; i >= 0; i--) {
            Node node = diagrams.item(i);
            if (node.getParentNode() != null) {
                node.getParentNode().removeChild(node);
            }
        }
    }

    private static String toXml(Document document) throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        var transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(writer));
        return writer.toString();
    }

    private static int findProcessStart(String xml) {
        var matcher = PROCESS_TAG.matcher(xml);
        return matcher.find() ? matcher.start() : -1;
    }

    private static String upsertAttribute(String tag, Pattern attributePattern, String attributeName, String value) {
        var matcher = attributePattern.matcher(tag);
        if (matcher.find()) {
            return matcher.replaceFirst(" " + attributeName + "=\"" + escapeAttribute(value) + "\"");
        }
        return tag.substring(0, tag.length() - 1) + " " + attributeName + "=\"" + escapeAttribute(value) + "\">";
    }

    private static String extractAttributeValue(String tag, String attributeName) {
        Pattern pattern = Pattern.compile("\\s" + Pattern.quote(attributeName) + "\\s*=\\s*\"([^\"]*)\"");
        var matcher = pattern.matcher(tag);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String escapeAttribute(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
