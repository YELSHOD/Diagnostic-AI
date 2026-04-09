package com.yelshod.diagnosticserviceai.ws;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogsWebSocketHandler extends TextWebSocketHandler {

    private final LogStreamSessionService logStreamSessionService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String containerId = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("containerId");
        if (containerId == null || containerId.isBlank()) {
            closeQuietly(session, CloseStatus.BAD_DATA);
            return;
        }
        logStreamSessionService.open(session.getId(), containerId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logStreamSessionService.close(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WS transport error for session {}", session.getId(), exception);
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignored) {
        }
    }
}
