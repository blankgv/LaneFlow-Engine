package com.laneflow.engine.modules.admin.response;

import java.time.LocalDateTime;
import java.util.List;

public record RoleResponse(
        String id,
        String code,
        String name,
        String description,
        List<String> permissions,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
