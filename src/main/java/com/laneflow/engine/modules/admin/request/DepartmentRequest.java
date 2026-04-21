package com.laneflow.engine.modules.admin.request;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        String parentId
) {}
