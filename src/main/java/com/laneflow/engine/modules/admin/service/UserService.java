package com.laneflow.engine.modules.admin.service;

import com.laneflow.engine.modules.admin.request.PasswordResetRequest;
import com.laneflow.engine.modules.admin.request.UserRequest;
import com.laneflow.engine.modules.admin.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse create(UserRequest request);
    List<UserResponse> getAll();
    UserResponse getById(String id);
    UserResponse update(String id, UserRequest request);
    void deactivate(String id);
    void resetPassword(String id, PasswordResetRequest request);
}
