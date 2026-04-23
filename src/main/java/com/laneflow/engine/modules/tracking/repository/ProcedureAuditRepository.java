package com.laneflow.engine.modules.tracking.repository;

import com.laneflow.engine.modules.tracking.model.ProcedureAudit;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedureAuditRepository extends MongoRepository<ProcedureAudit, String> {
    List<ProcedureAudit> findByProcedureIdOrderByCreatedAtAsc(String procedureId);
    Optional<ProcedureAudit> findTopByProcedureIdOrderByCreatedAtDesc(String procedureId);
}
