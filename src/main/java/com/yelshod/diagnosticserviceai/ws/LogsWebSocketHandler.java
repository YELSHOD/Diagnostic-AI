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
        String runtimeTargetId = UriComponentsBuilder.fromUri(session.getUri())
                .build()
                .getQueryParams()
                .getFirst("runtimeTargetId");
        if (runtimeTargetId == null || runtimeTargetId.isBlank()) {
            runtimeTargetId = UriComponentsBuilder.fromUri(session.getUri())
                    .build()
                    .getQueryParams()
                    .getFirst("containerId");
        }
        if (runtimeTargetId == null || runtimeTargetId.isBlank()) {
            log.warn("Websocket log stream rejected session={} reason=missing-runtime-target-id", session.getId());
            closeQuietly(session, CloseStatus.BAD_DATA);
            return;
        }
        log.info("Websocket log stream opened session={} runtimeTargetId={}", session.getId(), runtimeTargetId);
        logStreamSessionService.open(session.getId(), runtimeTargetId, session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("Websocket log stream closed session={} status={}", session.getId(), status);
        logStreamSessionService.close(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        if (isClientDisconnect(exception)) {
            log.info("Websocket transport closed session={} reason=client-disconnected message={}",
                    session.getId(), exception.getMessage());
            closeQuietly(session, CloseStatus.NORMAL);
            return;
        }
        log.error("Websocket transport error session={}", session.getId(), exception);
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    private boolean isClientDisconnect(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof IOException ioException && hasDisconnectMessage(ioException.getMessage())) {
                return true;
            }
            if (hasDisconnectMessage(current.getMessage())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean hasDisconnectMessage(String message) {
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("broken pipe")
                || normalized.contains("connection reset")
                || normalized.contains("closed channel")
                || normalized.contains("forcibly closed");
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignored) {
        }
    }
}
