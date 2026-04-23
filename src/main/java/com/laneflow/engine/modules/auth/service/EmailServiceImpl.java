package com.laneflow.engine.modules.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    @Override
    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetLink) {
        sendEmail(
                to,
                "LaneFlow - Recuperacion de contrasena",
                """
                Hola,

                Recibimos una solicitud para restablecer la contrasena de tu cuenta.
                Haz clic en el siguiente enlace (valido por 60 minutos):

                %s

                Si no solicitaste este cambio, ignora este mensaje.

                - Equipo LaneFlow
                """.formatted(resetLink)
        );
        log.info("Email de recuperacion enviado a: {}", to);
    }
}
