package com.laneflow.engine.core.config;

import com.laneflow.engine.core.security.JwtService;
import com.laneflow.engine.core.security.UserDetailsServiceImpl;
import com.laneflow.engine.core.security.UserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketAuthChannelInterceptorTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsServiceImpl userDetailsService = mock(UserDetailsServiceImpl.class);
    private final WebSocketAuthChannelInterceptor interceptor =
            new WebSocketAuthChannelInterceptor(jwtService, userDetailsService);

    @Test
    void rejectsConnectWithoutValidToken() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(IllegalStateException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void attachesAuthenticatedPrincipalOnValidConnect() {
        when(jwtService.isTokenValid("token-ok")).thenReturn(true);
        when(jwtService.extractUsername("token-ok")).thenReturn("admin");
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(new UserPrincipal(
                "user-1",
                "admin",
                "secret",
                "ADMINISTRADOR",
                true,
                List.of(new SimpleGrantedAuthority("WORKFLOW_WRITE"))
        ));

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer token-ok");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(message, null);

        verify(jwtService).extractUsername("token-ok");
        verify(userDetailsService).loadUserByUsername("admin");
    }
}
