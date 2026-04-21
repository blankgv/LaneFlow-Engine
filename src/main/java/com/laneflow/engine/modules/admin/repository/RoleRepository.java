package com.laneflow.engine.modules.admin.repository;

import com.laneflow.engine.modules.admin.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends MongoRepository<Role, String> {
    List<Role> findByActiveTrueOrderByCodeAsc();
    Optional<Role> findByCode(String code);
    boolean existsByCode(String code);
}
