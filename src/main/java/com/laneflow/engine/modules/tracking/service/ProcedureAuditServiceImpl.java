package com.laneflow.engine.modules.tracking.service;

import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import com.laneflow.engine.modules.tracking.model.ProcedureAudit;
import com.laneflow.engine.modules.tracking.model.enums.ProcedureAuditAction;
import com.laneflow.engine.modules.tracking.repository.ProcedureAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProcedureAuditServiceImpl implements ProcedureAuditService {

    private final ProcedureAuditRepository repository;

    @Override
    public void record(Procedure procedure,
                       ProcedureAuditAction action,
                       String description,
                       String username,
                       String taskId,
                       String nodeId,
                       String nodeName,
                       ProcedureStatus statusBefore,
                       ProcedureStatus statusAfter,
                       Map<String, Object> metadata) {
        repository.save(ProcedureAudit.builder()
                .procedureId(procedure.getId())
                .procedureCode(procedure.getCode())
                .action(action)
                .description(description)
                .username(username)
                .taskId(taskId)
                .nodeId(nodeId)
                .nodeName(nodeName)
                .statusBefore(statusBefore)
                .statusAfter(statusAfter)
                .metadata(metadata == null ? Map.of() : metadata)
                .build());
    }
}
