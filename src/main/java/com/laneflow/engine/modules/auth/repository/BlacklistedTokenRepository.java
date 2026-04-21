package com.laneflow.engine.modules.auth.repository;

import com.laneflow.engine.modules.auth.model.BlacklistedToken;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BlacklistedTokenRepository extends MongoRepository<BlacklistedToken, String> {
    boolean existsByJti(String jti);
}
