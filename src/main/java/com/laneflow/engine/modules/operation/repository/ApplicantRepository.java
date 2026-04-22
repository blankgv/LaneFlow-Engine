package com.laneflow.engine.modules.operation.repository;

import com.laneflow.engine.modules.operation.model.Applicant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicantRepository extends MongoRepository<Applicant, String> {
    List<Applicant> findByActiveTrueOrderByCreatedAtDesc();
    Optional<Applicant> findByDocumentNumber(String documentNumber);
    boolean existsByDocumentNumber(String documentNumber);
}
