package com.laneflow.engine.modules.workflow.service;

import java.util.regex.Pattern;

final class BpmnDeploymentPreparer {

    private static final Pattern DEFINITIONS_TAG = Pattern.compile("<([\\w.-]+:)?definitions\\b");
    private static final Pattern PROCESS_TAG = Pattern.compile("<([\\w.-]+:)?process\\b");
    private static final Pattern PROCESS_ID_ATTRIBUTE = Pattern.compile("\\sid\\s*=\\s*\"[^\"]*\"");
    private static final Pattern PROCESS_NAME_ATTRIBUTE = Pattern.compile("\\sname\\s*=\\s*\"[^\"]*\"");

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
        String updatedProcessTag = upsertAttribute(processTag, PROCESS_ID_ATTRIBUTE, "id", processKey);
        updatedProcessTag = upsertAttribute(updatedProcessTag, PROCESS_NAME_ATTRIBUTE, "name", processName);

        if (!updatedProcessTag.contains("camunda:historyTimeToLive=")) {
            updatedProcessTag = updatedProcessTag.substring(0, updatedProcessTag.length() - 1)
                    + " camunda:historyTimeToLive=\"180\">";
        }

        return prepared.substring(0, processStart)
                + updatedProcessTag
                + prepared.substring(processEnd + 1);
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

    private static String escapeAttribute(String value) {
        return value
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
