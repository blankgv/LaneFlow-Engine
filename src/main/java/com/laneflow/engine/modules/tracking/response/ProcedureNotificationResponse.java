package com.laneflow.engine.modules.tracking.response;

import com.laneflow.engine.modules.tracking.model.enums.NotificationChannel;
import com.laneflow.engine.modules.tracking.model.enums.NotificationStatus;
import com.laneflow.engine.modules.tracking.model.enums.NotificationType;

import java.time.LocalDateTime;

public record ProcedureNotificationResponse(
        String id,
        String procedureId,
        String procedureCode,
        NotificationType type,
        NotificationChannel channel,
        String recipient,
        String subject,
        String message,
        NotificationStatus status,
        String errorMessage,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
}
