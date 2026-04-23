package com.laneflow.engine.modules.tracking.service;

import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import com.laneflow.engine.modules.tracking.model.enums.ProcedureAuditAction;

import java.util.Map;

public interface ProcedureAuditService {
    void record(Procedure procedure,
                ProcedureAuditAction action,
                String description,
                String username,
                String taskId,
                String nodeId,
                String nodeName,
                ProcedureStatus statusBefore,
                ProcedureStatus statusAfter,
                Map<String, Object> metadata);
}
