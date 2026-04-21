package com.laneflow.engine.modules.admin.service;

import com.laneflow.engine.modules.admin.request.StaffRequest;
import com.laneflow.engine.modules.admin.response.StaffResponse;

import java.util.List;

public interface StaffService {
    StaffResponse create(StaffRequest request);
    List<StaffResponse> getAll();
    StaffResponse getById(String id);
    StaffResponse update(String id, StaffRequest request);
    void deactivate(String id);
}
