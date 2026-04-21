package com.laneflow.engine.modules.auth.service;

public interface EmailService {
    void sendPasswordResetEmail(String to, String resetLink);
}
