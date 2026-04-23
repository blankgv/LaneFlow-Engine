package com.laneflow.engine.modules.tracking.repository;

import com.laneflow.engine.modules.tracking.model.ProcedureNotification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ProcedureNotificationRepository extends MongoRepository<ProcedureNotification, String> {
    List<ProcedureNotification> findByProcedureIdOrderByCreatedAtDesc(String procedureId);
}
