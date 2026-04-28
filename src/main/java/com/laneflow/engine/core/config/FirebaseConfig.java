package com.laneflow.engine.core.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials-file:}")
    private String credentialsFile;

    @PostConstruct
    public void init() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try {
            GoogleCredentials credentials;
            if (credentialsFile != null && !credentialsFile.isBlank()) {
                // Local dev: JSON key file
                try (var stream = new FileInputStream(credentialsFile)) {
                    credentials = GoogleCredentials.fromStream(stream);
                }
                log.info("Firebase initialized with credentials file");
            } else {
                // Cloud Run / GCP: Application Default Credentials via IAM
                credentials = GoogleCredentials.getApplicationDefault();
                log.info("Firebase initialized with ADC (IAM)");
            }
            FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .build());
        } catch (IOException e) {
            log.error("Failed to initialize Firebase: {}", e.getMessage());
        }
    }
}
