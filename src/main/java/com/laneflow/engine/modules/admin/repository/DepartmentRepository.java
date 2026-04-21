package com.laneflow.engine.modules.admin.repository;

import com.laneflow.engine.modules.admin.model.Department;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends MongoRepository<Department, String> {
    List<Department> findByActiveTrueOrderByCodeAsc();
    Optional<Department> findByCode(String code);
    boolean existsByCode(String code);
    List<Department> findByParentIdAndActiveTrue(String parentId);
}
