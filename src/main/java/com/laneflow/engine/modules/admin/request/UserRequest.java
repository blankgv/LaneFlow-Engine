package com.laneflow.engine.modules.admin.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank String username,
        String password,
        @Email @NotBlank String email,
        @NotBlank String staffId,
        @NotBlank String roleId
) {}
