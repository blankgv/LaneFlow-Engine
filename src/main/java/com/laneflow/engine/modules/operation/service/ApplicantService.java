package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.operation.request.ApplicantRequest;
import com.laneflow.engine.modules.operation.response.ApplicantResponse;

import java.util.List;

public interface ApplicantService {
    ApplicantResponse create(ApplicantRequest request);
    List<ApplicantResponse> getAll();
    ApplicantResponse getById(String id);
    ApplicantResponse update(String id, ApplicantRequest request);
    void deactivate(String id);
}
