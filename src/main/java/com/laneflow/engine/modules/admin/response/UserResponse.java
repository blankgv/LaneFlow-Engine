package com.laneflow.engine.modules.admin.response;

import java.time.LocalDateTime;

public record UserResponse(
        String id,
        String username,
        String email,
        String staffId,
        String staffCode,
        String staffFullName,
        String roleId,
        String roleCode,
        String roleName,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastLoginAt
) {}
