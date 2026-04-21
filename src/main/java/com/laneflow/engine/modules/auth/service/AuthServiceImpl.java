package com.laneflow.engine.modules.auth.service;

import com.laneflow.engine.core.security.JwtService;
import com.laneflow.engine.core.security.UserPrincipal;
import com.laneflow.engine.modules.admin.model.User;
import com.laneflow.engine.modules.admin.repository.UserRepository;
import com.laneflow.engine.modules.auth.model.BlacklistedToken;
import com.laneflow.engine.modules.auth.model.PasswordResetToken;
import com.laneflow.engine.modules.auth.repository.BlacklistedTokenRepository;
import com.laneflow.engine.modules.auth.repository.PasswordResetTokenRepository;
import com.laneflow.engine.modules.auth.request.LoginRequest;
import com.laneflow.engine.modules.auth.request.RecoverRequest;
import com.laneflow.engine.modules.auth.request.ResetPasswordRequest;
import com.laneflow.engine.modules.auth.response.LoginResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.mail.reset-password-url}")
    private String resetPasswordUrl;

    @Value("${app.reset-token.expiration-minutes}")
    private int resetTokenExpirationMinutes;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = jwtService.generateToken(principal);

        userRepository.findByUsername(principal.getUsername()).ifPresent(user -> {
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
        });

        List<String> permissions = principal.authorities().stream()
                .map(a -> a.getAuthority())
                .toList();

        return new LoginResponse(token, principal.getUsername(), principal.roleCode(),
                permissions, expirationMs);
    }

    @Override
    public void logout(String token) {
        if (!jwtService.isTokenValid(token)) return;

        Claims claims = jwtService.extractAllClaims(token);
        blacklistedTokenRepository.save(BlacklistedToken.builder()
                .jti(claims.get("jti", String.class))
                .expiresAt(claims.getExpiration())
                .build());
    }

    @Override
    public void requestPasswordRecovery(RecoverRequest request) {
        // Si el email no existe no revelamos si está registrado o no (seguridad)
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            Date expiresAt = new Date(System.currentTimeMillis() +
                    (long) resetTokenExpirationMinutes * 60 * 1000);

            resetTokenRepository.save(PasswordResetToken.builder()
                    .token(token)
                    .email(request.email())
                    .expiresAt(expiresAt)
                    .build());

            String resetLink = resetPasswordUrl + "?token=" + token;
            emailService.sendPasswordResetEmail(request.email(), resetLink);
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = resetTokenRepository
                .findByTokenAndUsedFalse(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o ya utilizado."));

        if (resetToken.getExpiresAt().before(new Date()))
            throw new IllegalArgumentException("El token ha expirado.");

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }
}
