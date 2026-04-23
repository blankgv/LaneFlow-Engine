package com.laneflow.engine.modules.tracking.service;

import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.tracking.model.enums.NotificationType;
import com.laneflow.engine.modules.tracking.response.ProcedureNotificationResponse;

import java.util.List;

public interface ProcedureNotificationService {
    void notifyApplicant(Procedure procedure, NotificationType type);
    List<ProcedureNotificationResponse> getByProcedure(String procedureId);
}
