package com.laneflow.engine.modules.tracking.request;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenRequest(
        @NotBlank String token,
        String platform
) {}
