package com.laneflow.engine.modules.tracking.service;

public interface FcmService {
    void sendToUser(String username, String title, String body, String procedureId);
    void sendToApplicant(String applicantId, String title, String body, String procedureId);
    void registerToken(String username, String token, String platform);
}
