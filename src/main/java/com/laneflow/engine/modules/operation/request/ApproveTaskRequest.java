package com.laneflow.engine.modules.operation.request;

import java.util.Map;

public record ApproveTaskRequest(
        String comment,
        Map<String, Object> formData
) {}
