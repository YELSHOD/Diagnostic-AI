package com.yelshod.diagnosticserviceai.config;

import com.yelshod.diagnosticserviceai.ws.LogsWebSocketHandler;
import com.yelshod.diagnosticserviceai.ws.WebSocketAuthHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final LogsWebSocketHandler logsWebSocketHandler;
    private final WebSocketAuthHandshakeInterceptor webSocketAuthHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logsWebSocketHandler, "/ws/logs")
                .addInterceptors(webSocketAuthHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
