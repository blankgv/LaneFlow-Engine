package com.laneflow.engine.modules.workflow.response;

import com.laneflow.engine.modules.workflow.model.enums.WorkflowStatus;

import java.time.LocalDateTime;

public record WorkflowSummaryResponse(
        String id,
        String code,
        String name,
        String description,
        WorkflowStatus status,
        int currentVersion,
        Integer publishedVersionNumber,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
