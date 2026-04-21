package com.laneflow.engine.modules.admin.service;

import com.laneflow.engine.modules.admin.request.RoleRequest;
import com.laneflow.engine.modules.admin.response.RoleResponse;

import java.util.List;

public interface RoleService {
    RoleResponse create(RoleRequest request);
    List<RoleResponse> getAll();
    RoleResponse getById(String id);
    RoleResponse update(String id, RoleRequest request);
    void deactivate(String id);
}
