package com.laneflow.engine.core.config;

import com.laneflow.engine.core.security.JwtService;
import com.laneflow.engine.core.security.UserDetailsServiceImpl;
import com.laneflow.engine.core.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            if (accessor.getUser() != null && accessor.getUser().getName() != null
                    && !accessor.getUser().getName().isBlank()) {
                return message;
            }

            String token = resolveBearerToken(accessor.getNativeHeader("Authorization"));
            if (token == null || !jwtService.isTokenValid(token)) {
                throw new IllegalStateException("La conexion WebSocket requiere un token valido.");
            }

            String username = jwtService.extractUsername(token);
            UserPrincipal principal = userDetailsService.loadUserByUsername(username);
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    principal.getAuthorities()
            ));
        }
        return message;
    }

    private String resolveBearerToken(List<String> authorizationHeaders) {
        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            return null;
        }

        for (String header : authorizationHeaders) {
            if (header != null && header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }
        return null;
    }
}
