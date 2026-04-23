package com.laneflow.engine.modules.tracking.repository;

import com.laneflow.engine.modules.tracking.model.ProcedureAudit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProcedureAuditRepository extends MongoRepository<ProcedureAudit, String> {
    List<ProcedureAudit> findByProcedureIdOrderByCreatedAtAsc(String procedureId);
}
