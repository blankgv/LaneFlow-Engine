package com.laneflow.engine.modules.tracking.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.tracking.model.FcmToken;
import com.laneflow.engine.modules.tracking.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService {

    private final FcmTokenRepository fcmTokenRepository;
    private final UserRepository userRepository;

    @Override
    public void registerToken(String username, String token, String platform) {
        FcmToken fcmToken = fcmTokenRepository.findByUsername(username)
                .orElseGet(() -> FcmToken.builder().username(username).build());
        fcmToken.setToken(token);
        fcmToken.setPlatform(platform);
        fcmToken.setUpdatedAt(LocalDateTime.now());
        fcmTokenRepository.save(fcmToken);
    }

    @Override
    public void sendToUser(String username, String title, String body, String procedureId) {
        fcmTokenRepository.findByUsername(username).ifPresent(fcmToken ->
                sendPush(fcmToken.getToken(), title, body, procedureId)
        );
    }

    @Override
    public void sendToApplicant(String applicantId, String title, String body, String procedureId) {
        userRepository.findByApplicantId(applicantId).ifPresent(user ->
                sendToUser(user.getUsername(), title, body, procedureId)
        );
    }

    private void sendPush(String token, String title, String body, String procedureId) {
        if (FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try {
            Message message = Message.builder()
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("procedureId", procedureId != null ? procedureId : "")
                    .setToken(token)
                    .build();
            FirebaseMessaging.getInstance().send(message);
            log.debug("FCM sent to token ending ...{}", token.substring(Math.max(0, token.length() - 6)));
        } catch (Exception e) {
            log.warn("FCM send failed: {}", e.getMessage());
        }
    }
}
