package com.laneflow.engine.modules.tracking.controller;

import com.laneflow.engine.modules.tracking.request.FcmTokenRequest;
import com.laneflow.engine.modules.tracking.service.FcmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class FcmTokenController {

    private final FcmService fcmService;

    @PostMapping("/fcm-token")
    public ResponseEntity<Void> registerToken(
            @Valid @RequestBody FcmTokenRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        fcmService.registerToken(
                userDetails.getUsername(),
                request.token(),
                request.platform() != null ? request.platform() : "android"
        );
        return ResponseEntity.noContent().build();
    }
}
