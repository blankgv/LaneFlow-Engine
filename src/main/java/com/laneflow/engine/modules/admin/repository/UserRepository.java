package com.laneflow.engine.modules.admin.repository;

import com.laneflow.engine.modules.admin.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);
    List<User> findByActiveTrueOrderByUsernameAsc();
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByApplicantId(String applicantId);
}
