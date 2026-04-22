package com.laneflow.engine.modules.operation.repository;

import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.model.enums.ProcedureStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ProcedureRepository extends MongoRepository<Procedure, String> {
    List<Procedure> findAllByOrderByCreatedAtDesc();
    List<Procedure> findByApplicantIdOrderByCreatedAtDesc(String applicantId);
    List<Procedure> findByStatusOrderByCreatedAtDesc(ProcedureStatus status);
    Optional<Procedure> findByCamundaProcessInstanceId(String camundaProcessInstanceId);
    boolean existsByCode(String code);
}
