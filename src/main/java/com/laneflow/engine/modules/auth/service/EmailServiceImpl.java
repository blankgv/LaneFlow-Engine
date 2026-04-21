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
    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("LaneFlow — Recuperación de contraseña");
        message.setText("""
                Hola,

                Recibimos una solicitud para restablecer la contraseña de tu cuenta.
                Haz clic en el siguiente enlace (válido por 60 minutos):

                %s

                Si no solicitaste este cambio, ignora este mensaje.

                — Equipo LaneFlow
                """.formatted(resetLink));

        mailSender.send(message);
        log.info("Email de recuperación enviado a: {}", to);
    }
}
