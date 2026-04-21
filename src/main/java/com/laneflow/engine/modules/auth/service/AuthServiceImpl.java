package com.laneflow.engine.modules.auth.service;

import com.laneflow.engine.core.security.JwtService;
import com.laneflow.engine.core.security.UserPrincipal;
import com.laneflow.engine.modules.auth.model.BlacklistedToken;
import com.laneflow.engine.modules.auth.repository.BlacklistedTokenRepository;
import com.laneflow.engine.modules.auth.request.LoginRequest;
import com.laneflow.engine.modules.auth.response.LoginResponse;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = jwtService.generateToken(principal);

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
}
