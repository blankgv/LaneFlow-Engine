package com.laneflow.engine.modules.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    private String id;

    private String token;
    private String email;

    @Builder.Default
    private boolean used = false;

    // MongoDB elimina el documento automáticamente al expirar
    @Indexed(expireAfterSeconds = 0)
    private Date expiresAt;
}
