package com.laneflow.engine.modules.workflow.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderFieldsRequest(@NotEmpty List<FieldOrderItem> fields) {

    public record FieldOrderItem(String fieldId, int order) {}
}
