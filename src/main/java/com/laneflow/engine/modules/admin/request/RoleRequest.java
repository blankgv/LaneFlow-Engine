package com.laneflow.engine.modules.admin.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RoleRequest(
        @NotBlank String code,
        @NotBlank String name,
        String description,
        @NotEmpty List<String> permissions
) {}
