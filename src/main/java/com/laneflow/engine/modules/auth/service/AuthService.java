package com.laneflow.engine.modules.auth.service;

import com.laneflow.engine.modules.auth.request.LoginRequest;
import com.laneflow.engine.modules.auth.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
}
