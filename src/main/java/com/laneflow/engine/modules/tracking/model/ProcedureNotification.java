package com.laneflow.engine.modules.tracking.model;

import com.laneflow.engine.modules.tracking.model.enums.NotificationChannel;
import com.laneflow.engine.modules.tracking.model.enums.NotificationStatus;
import com.laneflow.engine.modules.tracking.model.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "procedure_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureNotification {

    @Id
    private String id;

    @Indexed
    private String procedureId;

    private String procedureCode;
    private NotificationType type;
    private NotificationChannel channel;
    private String recipient;
    private String subject;
    private String message;
    private NotificationStatus status;
    private String errorMessage;
    private LocalDateTime sentAt;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
