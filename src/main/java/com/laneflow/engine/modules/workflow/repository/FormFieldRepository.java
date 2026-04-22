package com.laneflow.engine.modules.workflow.repository;

import com.laneflow.engine.modules.workflow.model.FormField;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FormFieldRepository extends MongoRepository<FormField, String> {

    List<FormField> findByFormIdOrderByOrderAsc(String formId);

    void deleteByFormId(String formId);

    long countByFormId(String formId);
}
