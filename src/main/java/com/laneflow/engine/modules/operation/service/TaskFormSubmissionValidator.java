package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.operation.model.Evidence;
import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.repository.EvidenceRepository;
import com.laneflow.engine.modules.workflow.model.embedded.FieldValidation;
import com.laneflow.engine.modules.workflow.model.enums.FieldType;
import com.laneflow.engine.modules.workflow.response.DynamicFormResponse;
import com.laneflow.engine.modules.workflow.response.FormFieldResponse;
import com.laneflow.engine.modules.workflow.service.DynamicFormService;
import lombok.RequiredArgsConstructor;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class TaskFormSubmissionValidator {

    private static final Set<FieldType> FILE_FIELD_TYPES = Set.of(
            FieldType.FILE,
            FieldType.IMAGE,
            FieldType.PHOTO,
            FieldType.AUDIO,
            FieldType.DOCUMENT,
            FieldType.VIDEO
    );

    private final DynamicFormService dynamicFormService;
    private final EvidenceRepository evidenceRepository;

    public void validate(Procedure procedure,
                         Task task,
                         Map<String, Object> submittedData,
                         Map<String, Object> mergedData) {
        validate(procedure, task.getId(), task.getTaskDefinitionKey(), submittedData, mergedData);
    }

    public void validate(Procedure procedure,
                         String taskId,
                         String nodeId,
                         Map<String, Object> submittedData,
                         Map<String, Object> mergedData) {
        DynamicFormResponse form = dynamicFormService
                .findOptionalByWorkflowAndNode(procedure.getWorkflowDefinitionId(), nodeId)
                .orElse(null);

        if (form == null) {
            return;
        }

        Map<String, Object> safeSubmitted = submittedData == null ? Collections.emptyMap() : submittedData;
        Map<String, Object> safeMerged = mergedData == null ? Collections.emptyMap() : mergedData;
        Set<String> allowedFields = new LinkedHashSet<>();
        for (FormFieldResponse field : form.fields()) {
            allowedFields.add(field.name());
        }

        List<String> unexpectedFields = safeSubmitted.keySet().stream()
                .filter(key -> !allowedFields.contains(key))
                .sorted()
                .toList();
        if (!unexpectedFields.isEmpty()) {
            throw new IllegalArgumentException("El formulario de la tarea no admite estos campos: "
                    + String.join(", ", unexpectedFields));
        }

        List<Evidence> taskEvidences = evidenceRepository.findByProcedureIdAndTaskIdAndNodeIdOrderByCreatedAtDesc(
                procedure.getId(),
                taskId,
                nodeId
        );

        for (FormFieldResponse field : form.fields()) {
            Object value = safeMerged.get(field.name());
            validateRequired(field, value, taskEvidences);
            if (value == null) {
                continue;
            }
            validateFieldType(field, value);
            validateFieldOptions(field, value);
            validateCustomRules(field, value);
        }
    }

    private void validateRequired(FormFieldResponse field, Object value, List<Evidence> taskEvidences) {
        if (!field.required()) {
            return;
        }

        if (FILE_FIELD_TYPES.contains(field.type())) {
            boolean hasEvidence = taskEvidences.stream()
                    .anyMatch(evidence -> Objects.equals(field.name(), evidence.getFieldName()));
            if (!hasEvidence) {
                throw new IllegalArgumentException("El campo de archivo es obligatorio: " + field.label());
            }
            return;
        }

        if (isEmptyValue(value)) {
            throw new IllegalArgumentException("El campo es obligatorio: " + field.label());
        }
    }

    private void validateFieldType(FormFieldResponse field, Object value) {
        switch (field.type()) {
            case TEXT, TEXTAREA, DATE, DATETIME -> {
                if (!(value instanceof String)) {
                    throw invalidType(field);
                }
            }
            case NUMBER -> {
                if (!(value instanceof Number) && !isParsableNumber(value)) {
                    throw invalidType(field);
                }
            }
            case SELECT, RADIO -> {
                if (!(value instanceof String)) {
                    throw invalidType(field);
                }
            }
            case MULTISELECT -> {
                if (!(value instanceof Collection<?>)) {
                    throw invalidType(field);
                }
            }
            case CHECKBOX -> {
                if (!(value instanceof Boolean)) {
                    throw invalidType(field);
                }
            }
            case FILE, IMAGE, PHOTO, AUDIO, DOCUMENT, VIDEO, GENERIC -> {
            }
        }
    }

    private void validateFieldOptions(FormFieldResponse field, Object value) {
        if (field.options() == null || field.options().isEmpty()) {
            return;
        }

        switch (field.type()) {
            case SELECT, RADIO -> {
                if (!field.options().contains(String.valueOf(value))) {
                    throw new IllegalArgumentException("El valor no es valido para el campo: " + field.label());
                }
            }
            case MULTISELECT -> {
                Collection<?> values = (Collection<?>) value;
                List<String> invalidValues = values.stream()
                        .map(String::valueOf)
                        .filter(option -> !field.options().contains(option))
                        .toList();
                if (!invalidValues.isEmpty()) {
                    throw new IllegalArgumentException("El campo " + field.label()
                            + " contiene opciones invalidas: " + String.join(", ", invalidValues));
                }
            }
            default -> {
            }
        }
    }

    private void validateCustomRules(FormFieldResponse field, Object value) {
        if (field.validations() == null || field.validations().isEmpty()) {
            return;
        }

        for (FieldValidation rule : field.validations()) {
            if (rule == null || rule.getType() == null || rule.getType().isBlank()) {
                continue;
            }

            String type = rule.getType().trim();
            String message = rule.getMessage() == null || rule.getMessage().isBlank()
                    ? defaultValidationMessage(field, type)
                    : rule.getMessage().trim();

            switch (type) {
                case "minLength" -> {
                    int minLength = parseInt(rule.getValue(), type, field.label());
                    if (stringValue(value).length() < minLength) {
                        throw new IllegalArgumentException(message);
                    }
                }
                case "maxLength" -> {
                    int maxLength = parseInt(rule.getValue(), type, field.label());
                    if (stringValue(value).length() > maxLength) {
                        throw new IllegalArgumentException(message);
                    }
                }
                case "pattern" -> {
                    if (!Pattern.compile(rule.getValue()).matcher(stringValue(value)).matches()) {
                        throw new IllegalArgumentException(message);
                    }
                }
                case "min" -> {
                    double min = parseDouble(rule.getValue(), type, field.label());
                    if (numberValue(value) < min) {
                        throw new IllegalArgumentException(message);
                    }
                }
                case "max" -> {
                    double max = parseDouble(rule.getValue(), type, field.label());
                    if (numberValue(value) > max) {
                        throw new IllegalArgumentException(message);
                    }
                }
                default -> {
                }
            }
        }
    }

    private IllegalArgumentException invalidType(FormFieldResponse field) {
        return new IllegalArgumentException("Tipo de dato invalido para el campo: " + field.label());
    }

    private boolean isEmptyValue(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

    private boolean isParsableNumber(Object value) {
        try {
            Double.parseDouble(String.valueOf(value));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String stringValue(Object value) {
        return String.valueOf(value);
    }

    private double numberValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(value));
    }

    private int parseInt(String raw, String type, String label) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Configuracion invalida de " + type + " para el campo: " + label, e);
        }
    }

    private double parseDouble(String raw, String type, String label) {
        try {
            return Double.parseDouble(raw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Configuracion invalida de " + type + " para el campo: " + label, e);
        }
    }

    private String defaultValidationMessage(FormFieldResponse field, String type) {
        return "El campo " + field.label() + " no cumple la regla " + type + ".";
    }
}
