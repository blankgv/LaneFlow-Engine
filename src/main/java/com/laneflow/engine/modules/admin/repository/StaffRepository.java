package com.laneflow.engine.modules.admin.repository;

import com.laneflow.engine.modules.admin.model.Staff;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StaffRepository extends MongoRepository<Staff, String> {
    List<Staff> findByActiveTrueOrderByCodeAsc();
    List<Staff> findByDepartmentIdAndActiveTrue(String departmentId);
    Optional<Staff> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, String id);
    boolean existsByCodeAndIdNot(String code, String id);
}
