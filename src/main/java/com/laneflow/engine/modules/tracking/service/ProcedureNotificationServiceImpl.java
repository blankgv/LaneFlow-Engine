package com.laneflow.engine.modules.tracking.service;

import com.laneflow.engine.modules.auth.service.EmailService;
import com.laneflow.engine.modules.operation.model.Applicant;
import com.laneflow.engine.modules.operation.model.Procedure;
import com.laneflow.engine.modules.operation.repository.ApplicantRepository;
import com.laneflow.engine.modules.operation.repository.ProcedureRepository;
import com.laneflow.engine.modules.tracking.model.ProcedureNotification;
import com.laneflow.engine.modules.tracking.model.enums.NotificationChannel;
import com.laneflow.engine.modules.tracking.model.enums.NotificationStatus;
import com.laneflow.engine.modules.tracking.model.enums.NotificationType;
import com.laneflow.engine.modules.tracking.repository.ProcedureNotificationRepository;
import com.laneflow.engine.modules.tracking.response.ProcedureNotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcedureNotificationServiceImpl implements ProcedureNotificationService {

    private final ProcedureNotificationRepository repository;
    private final ProcedureRepository procedureRepository;
    private final ApplicantRepository applicantRepository;
    private final EmailService emailService;
    private final FcmService fcmService;

    @Override
    public void notifyApplicant(Procedure procedure, NotificationType type) {
        Applicant applicant = applicantRepository.findById(procedure.getApplicantId()).orElse(null);
        String recipient = applicant == null ? null : trimToNull(applicant.getEmail());
        String subject = buildSubject(procedure, type);
        String message = buildMessage(procedure, applicant, type);

        ProcedureNotification notification = ProcedureNotification.builder()
                .procedureId(procedure.getId())
                .procedureCode(procedure.getCode())
                .type(type)
                .channel(NotificationChannel.EMAIL)
                .recipient(recipient)
                .subject(subject)
                .message(message)
                .status(NotificationStatus.PENDING)
                .build();

        if (recipient == null) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage("El solicitante no tiene un email configurado.");
            repository.save(notification);
        } else {
            try {
                emailService.sendEmail(recipient, subject, message);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } catch (Exception ex) {
                log.warn("No se pudo enviar notificacion {} del tramite {}", type, procedure.getId(), ex);
                notification.setStatus(NotificationStatus.FAILED);
                notification.setErrorMessage(ex.getMessage());
            }
            repository.save(notification);
        }

        // Push notification siempre (independiente del email)
        String pushTitle = buildSubject(procedure, type).replace("LaneFlow - ", "");
        String pushBody = "Trámite %s — %s".formatted(procedure.getCode(), buildPushBody(type));
        fcmService.sendToApplicant(procedure.getApplicantId(), pushTitle, pushBody, procedure.getId());
    }

    @Override
    public List<ProcedureNotificationResponse> getByProcedure(String procedureId) {
        if (!procedureRepository.existsById(procedureId)) {
            throw new IllegalArgumentException("Tramite no encontrado: " + procedureId);
        }

        return repository.findByProcedureIdOrderByCreatedAtDesc(procedureId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private String buildSubject(Procedure procedure, NotificationType type) {
        return switch (type) {
            case PROCEDURE_STARTED -> "LaneFlow - Tu tramite fue iniciado";
            case PROCEDURE_APPROVED -> "LaneFlow - Tu tramite fue aprobado";
            case PROCEDURE_OBSERVED -> "LaneFlow - Tu tramite tiene observaciones";
            case PROCEDURE_REJECTED -> "LaneFlow - Tu tramite fue rechazado";
            case OBSERVATION_RESOLVED -> "LaneFlow - Tu tramite fue reingresado al flujo";
        };
    }

    private String buildMessage(Procedure procedure, Applicant applicant, NotificationType type) {
        String applicantName = applicant == null ? procedure.getApplicantName() : resolveApplicantName(applicant, procedure);
        String greeting = applicantName == null || applicantName.isBlank() ? "Hola" : "Hola %s".formatted(applicantName);

        String body = switch (type) {
            case PROCEDURE_STARTED ->
                    "Tu tramite %s fue iniciado correctamente y ya ingreso al flujo %s.".formatted(
                            procedure.getCode(), procedure.getWorkflowName());
            case PROCEDURE_APPROVED ->
                    "Tu tramite %s fue aprobado.".formatted(procedure.getCode());
            case PROCEDURE_OBSERVED ->
                    "Tu tramite %s tiene observaciones y requiere una subsanacion.".formatted(procedure.getCode());
            case PROCEDURE_REJECTED ->
                    "Tu tramite %s fue rechazado.".formatted(procedure.getCode());
            case OBSERVATION_RESOLVED ->
                    "La observacion de tu tramite %s fue subsanada y el tramite fue reenviado al flujo.".formatted(
                            procedure.getCode());
        };

        return """
                %s,

                %s

                Estado actual: %s
                Flujo: %s

                - Equipo LaneFlow
                """.formatted(greeting, body, procedure.getStatus(), procedure.getWorkflowName());
    }

    private String resolveApplicantName(Applicant applicant, Procedure procedure) {
        if (applicant.getBusinessName() != null && !applicant.getBusinessName().isBlank()) {
            return applicant.getBusinessName().trim();
        }
        String fullName = ((applicant.getFirstName() == null ? "" : applicant.getFirstName().trim()) + " "
                + (applicant.getLastName() == null ? "" : applicant.getLastName().trim())).trim();
        return fullName.isBlank() ? procedure.getApplicantName() : fullName;
    }

    private String buildPushBody(NotificationType type) {
        return switch (type) {
            case PROCEDURE_STARTED -> "fue iniciado y enviado al flujo.";
            case PROCEDURE_APPROVED -> "fue aprobado.";
            case PROCEDURE_OBSERVED -> "tiene observaciones pendientes.";
            case PROCEDURE_REJECTED -> "fue rechazado.";
            case OBSERVATION_RESOLVED -> "fue reingresado al flujo.";
        };
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ProcedureNotificationResponse toResponse(ProcedureNotification notification) {
        return new ProcedureNotificationResponse(
                notification.getId(),
                notification.getProcedureId(),
                notification.getProcedureCode(),
                notification.getType(),
                notification.getChannel(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getMessage(),
                notification.getStatus(),
                notification.getErrorMessage(),
                notification.getSentAt(),
                notification.getCreatedAt()
        );
    }
}
