package com.laneflow.engine.modules.tracking.repository;

import com.laneflow.engine.modules.tracking.model.FcmToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FcmTokenRepository extends MongoRepository<FcmToken, String> {
    Optional<FcmToken> findByUsername(String username);
}
