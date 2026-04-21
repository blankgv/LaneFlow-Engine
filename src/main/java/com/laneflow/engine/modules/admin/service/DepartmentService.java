package com.laneflow.engine.modules.admin.service;

import com.laneflow.engine.modules.admin.request.DepartmentRequest;
import com.laneflow.engine.modules.admin.response.DepartmentResponse;

import java.util.List;

public interface DepartmentService {
    DepartmentResponse create(DepartmentRequest request);
    List<DepartmentResponse> getAll();
    DepartmentResponse getById(String id);
    DepartmentResponse update(String id, DepartmentRequest request);
    void deactivate(String id);
}
