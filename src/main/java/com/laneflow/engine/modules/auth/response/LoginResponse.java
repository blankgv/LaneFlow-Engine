package com.laneflow.engine.modules.auth.response;

import java.util.List;

public record LoginResponse(
        String token,
        String username,
        String roleCode,
        List<String> permissions,
        long expiresIn
) {}
