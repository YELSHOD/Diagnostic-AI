package com.yelshod.diagnosticserviceai.ws;

import com.yelshod.diagnosticserviceai.docker.DockerLogSession;
import com.yelshod.diagnosticserviceai.docker.DockerLogsService;
import com.yelshod.diagnosticserviceai.logs.LogProcessingService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
@RequiredArgsConstructor
public class LogStreamSessionService {

    private final DockerLogsService dockerLogsService;
    private final LogProcessingService logProcessingService;
    private final WsMessageSender wsMessageSender;
    private final Map<String, DockerLogSession> subscriptions = new ConcurrentHashMap<>();

    public void open(String sessionId, String containerId, WebSocketSession session) {
        DockerLogSession logSession = dockerLogsService.streamLogs(containerId, line -> {
            wsMessageSender.send(session, logProcessingService.toLogMessage(line));
            logProcessingService.maybeBuildErrorEvent(line, errorEvent -> {
                wsMessageSender.send(session, logProcessingService.toErrorMessage(errorEvent));
                wsMessageSender.send(session, logProcessingService.toClusterUpdate(errorEvent));
            }).ifPresent(errorEvent -> {
                wsMessageSender.send(session, logProcessingService.toErrorMessage(errorEvent));
                wsMessageSender.send(session, logProcessingService.toClusterUpdate(errorEvent));
            });
        });
        subscriptions.put(sessionId, logSession);
    }

    public void close(String sessionId) {
        DockerLogSession logSession = subscriptions.remove(sessionId);
        if (logSession != null) {
            logSession.close();
        }
    }
}
