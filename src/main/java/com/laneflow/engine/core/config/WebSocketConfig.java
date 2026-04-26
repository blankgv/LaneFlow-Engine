package com.laneflow.engine.core.config;

import com.laneflow.engine.core.common.ApiVersion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final List<String> allowedOrigins;
    private final List<String> allowedOriginPatterns;
    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

    public WebSocketConfig(
            WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor,
            @Value("${app.cors.allowed-origins}") String allowedOrigins,
            @Value("${app.cors.allowed-origin-patterns:}") String allowedOriginPatterns
    ) {
        this.webSocketAuthChannelInterceptor = webSocketAuthChannelInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        this.allowedOriginPatterns = Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();

        if (this.allowedOrigins.isEmpty() && this.allowedOriginPatterns.isEmpty()) {
            throw new IllegalStateException("WebSocket requires app.cors.allowed-origins or app.cors.allowed-origin-patterns to be configured.");
        }
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthChannelInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registerEndpoint(registry, "/ws");
        registerEndpoint(registry, ApiVersion.V1 + "/ws");
    }

    private void registerEndpoint(StompEndpointRegistry registry, String path) {
        var registration = registry.addEndpoint(path);

        if (!allowedOrigins.isEmpty()) {
            registration.setAllowedOrigins(allowedOrigins.toArray(String[]::new));
        }

        if (!allowedOriginPatterns.isEmpty()) {
            registration.setAllowedOriginPatterns(allowedOriginPatterns.toArray(String[]::new));
        }

        registration.withSockJS();
    }
}
