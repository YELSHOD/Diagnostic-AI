package com.yelshod.diagnosticserviceai.ws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.docker.DockerLogLine;
import com.yelshod.diagnosticserviceai.docker.DockerLogSession;
import com.yelshod.diagnosticserviceai.docker.DockerLogsService;
import com.yelshod.diagnosticserviceai.logs.LogProcessingService;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class LogStreamSessionServiceTest {

    @Mock
    private DockerLogsService dockerLogsService;

    @Mock
    private LogProcessingService logProcessingService;

    @Mock
    private WsMessageSender wsMessageSender;

    @Mock
    private WebSocketSession session;

    @Test
    void sendsTransformedLogMessageForIncomingDockerLine() {
        DockerLogSession dockerLogSession = () -> { };
        when(dockerLogsService.streamLogs(eq("container-1"), any())).thenReturn(dockerLogSession);

        WsMessage logMessage = new WsMessage(
                "LOG_LINE",
                Instant.parse("2026-04-09T10:00:00Z"),
                "orders",
                Map.of("message", "hello"));
        when(logProcessingService.toLogMessage(new DockerLogLine("orders", "hello"))).thenReturn(logMessage);

        LogStreamSessionService service = new LogStreamSessionService(dockerLogsService, logProcessingService, wsMessageSender);
        service.open("session-1", "container-1", session);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<DockerLogLine>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(dockerLogsService).streamLogs(eq("container-1"), captor.capture());
        captor.getValue().accept(new DockerLogLine("orders", "hello"));

        verify(wsMessageSender).send(session, logMessage);
    }
}
