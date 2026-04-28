package com.laneflow.engine.modules.tracking.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "fcm_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username;

    private String token;
    private String platform;

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
