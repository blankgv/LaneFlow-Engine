package com.laneflow.engine.modules.auth.service;

public interface EmailService {
    void sendEmail(String to, String subject, String text);
    void sendPasswordResetEmail(String to, String resetLink);
}
