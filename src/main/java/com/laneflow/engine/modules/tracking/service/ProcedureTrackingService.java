package com.laneflow.engine.modules.tracking.service;

import com.laneflow.engine.modules.tracking.response.ProcedureStatusResponse;

public interface ProcedureTrackingService {
    ProcedureStatusResponse getStatus(String procedureId);
}
