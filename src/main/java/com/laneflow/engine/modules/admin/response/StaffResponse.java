package com.laneflow.engine.modules.admin.response;

import java.time.LocalDateTime;

public record StaffResponse(
        String id,
        String code,
        String firstName,
        String lastName,
        String email,
        String phone,
        String departmentId,
        String departmentCode,
        String departmentName,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
