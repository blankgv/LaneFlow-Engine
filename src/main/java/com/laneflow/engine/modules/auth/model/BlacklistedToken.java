package com.laneflow.engine.modules.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "token_blacklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlacklistedToken {

    @Id
    private String id;

    private String jti;

    // MongoDB elimina el documento automáticamente cuando esta fecha pasa
    @Indexed(expireAfterSeconds = 0)
    private Date expiresAt;
}
