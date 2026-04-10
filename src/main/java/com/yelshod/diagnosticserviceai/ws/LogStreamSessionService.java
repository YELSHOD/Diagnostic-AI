package com.yelshod.diagnosticserviceai.ws;

import com.yelshod.diagnosticserviceai.logsource.LogSourceRouter;
import com.yelshod.diagnosticserviceai.logsource.LogSourceSession;
import com.yelshod.diagnosticserviceai.logs.LogProcessingService;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetDto;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
@RequiredArgsConstructor
public class LogStreamSessionService {

    private final RuntimeTargetService runtimeTargetService;
    private final LogSourceRouter logSourceRouter;
    private final LogProcessingService logProcessingService;
    private final WsMessageSender wsMessageSender;
    private final Map<String, LogSourceSession> subscriptions = new ConcurrentHashMap<>();

    public void open(String sessionId, String runtimeTargetId, WebSocketSession session) {
        RuntimeTargetDto runtimeTarget = runtimeTargetService.findRequiredTarget(runtimeTargetId);
        LogSourceSession logSession = logSourceRouter.open(runtimeTarget, line -> {
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
        LogSourceSession logSession = subscriptions.remove(sessionId);
        if (logSession != null) {
            logSession.close();
        }
    }
}
