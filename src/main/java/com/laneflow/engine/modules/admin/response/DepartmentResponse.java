package com.laneflow.engine.modules.admin.response;

import java.time.LocalDateTime;

public record DepartmentResponse(
        String id,
        String code,
        String name,
        String description,
        String parentId,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
