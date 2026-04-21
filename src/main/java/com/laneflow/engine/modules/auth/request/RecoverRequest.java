package com.laneflow.engine.modules.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecoverRequest(
        @Email @NotBlank String email
) {}
