package com.yelshod.diagnosticserviceai.ws;

import com.yelshod.diagnosticserviceai.logsource.LogSourceRouter;
import com.yelshod.diagnosticserviceai.logsource.LogSourceSession;
import com.yelshod.diagnosticserviceai.logs.LogProcessingService;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetDto;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetService;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

@Service
@Slf4j
@RequiredArgsConstructor
public class LogStreamSessionService {

    private final RuntimeTargetService runtimeTargetService;
    private final LogSourceRouter logSourceRouter;
    private final LogProcessingService logProcessingService;
    private final WsMessageSender wsMessageSender;
    private final Map<String, LogSourceSession> subscriptions = new ConcurrentHashMap<>();

    public void open(String sessionId, String runtimeTargetId, WebSocketSession session) {
        RuntimeTargetDto runtimeTarget = runtimeTargetService.findRequiredTarget(runtimeTargetId);
        log.debug("Registering websocket session={} runtimeTargetId={} runtimeTargetType={}",
                sessionId, runtimeTarget.id(), runtimeTarget.type());
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
        log.info("Websocket session registered session={} runtimeTargetId={}", sessionId, runtimeTarget.id());
    }

    public void close(String sessionId) {
        LogSourceSession logSession = subscriptions.remove(sessionId);
        if (logSession != null) {
            logSession.close();
            log.debug("Websocket session subscription closed session={}", sessionId);
        }
    }
}
