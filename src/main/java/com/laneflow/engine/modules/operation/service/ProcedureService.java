package com.laneflow.engine.modules.operation.service;

import com.laneflow.engine.modules.operation.request.ResolveObservationRequest;
import com.laneflow.engine.modules.operation.response.ProcedureResponse;
import com.laneflow.engine.modules.operation.request.StartProcedureRequest;

import java.util.List;

public interface ProcedureService {
    ProcedureResponse start(StartProcedureRequest request, String startedBy);
    ProcedureResponse resolveObservation(String id, ResolveObservationRequest request, String resolvedBy);
    List<ProcedureResponse> getAll();
    ProcedureResponse getById(String id);
}
