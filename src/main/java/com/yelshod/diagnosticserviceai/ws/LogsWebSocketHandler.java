package com.yelshod.diagnosticserviceai.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.docker.DockerLogSession;
import com.yelshod.diagnosticserviceai.docker.DockerLogsService;
import com.yelshod.diagnosticserviceai.logs.LogProcessingService;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogsWebSocketHandler extends TextWebSocketHandler {

    private final DockerLogsService dockerLogsService;
    private final LogProcessingService logProcessingService;
    private final ObjectMapper objectMapper;
    private final Map<String, DockerLogSession> subscriptions = new ConcurrentHashMap<>();

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

        DockerLogSession logSession = dockerLogsService.streamLogs(containerId, line -> {
            send(session, logProcessingService.toLogMessage(line));
            logProcessingService.maybeBuildErrorEvent(line, errorEvent -> {
                send(session, logProcessingService.toErrorMessage(errorEvent));
                send(session, logProcessingService.toClusterUpdate(errorEvent));
            }).ifPresent(errorEvent -> {
                send(session, logProcessingService.toErrorMessage(errorEvent));
                send(session, logProcessingService.toClusterUpdate(errorEvent));
            });
        });
        subscriptions.put(session.getId(), logSession);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        DockerLogSession logSession = subscriptions.remove(session.getId());
        if (logSession != null) {
            logSession.close();
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WS transport error for session {}", session.getId(), exception);
        closeQuietly(session, CloseStatus.SERVER_ERROR);
    }

    private void send(WebSocketSession session, WsMessage message) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(message);
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (JsonProcessingException e) {
            log.warn("Unable to serialize ws message", e);
        } catch (IOException e) {
            log.warn("Unable to send ws message", e);
        }
    }

    private void closeQuietly(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException ignored) {
        }
    }
}
