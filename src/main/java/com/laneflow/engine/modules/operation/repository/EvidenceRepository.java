package com.laneflow.engine.modules.operation.repository;

import com.laneflow.engine.modules.operation.model.Evidence;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EvidenceRepository extends MongoRepository<Evidence, String> {
    List<Evidence> findByProcedureIdOrderByCreatedAtDesc(String procedureId);
}
