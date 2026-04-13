package com.yelshod.diagnosticserviceai.ws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class LogsWebSocketHandlerTest {

    @Mock
    private LogStreamSessionService logStreamSessionService;

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private LogsWebSocketHandler logsWebSocketHandler;

    @Test
    void delegatesValidSubscriptionToSessionService() {
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws/logs?runtimeTargetId=abc123"));
        when(session.getId()).thenReturn("session-1");

        logsWebSocketHandler.afterConnectionEstablished(session);

        verify(logStreamSessionService).open(eq("session-1"), eq("abc123"), eq(session));
    }

    @Test
    void treatsBrokenPipeAsNormalClientDisconnect() throws Exception {
        when(session.getId()).thenReturn("session-1");

        logsWebSocketHandler.handleTransportError(session, new IOException("Broken pipe"));

        verify(session).close(eq(CloseStatus.NORMAL));
    }

    @Test
    void treatsUnexpectedTransportFailureAsServerError() throws Exception {
        when(session.getId()).thenReturn("session-1");

        logsWebSocketHandler.handleTransportError(session, new IllegalStateException("boom"));

        verify(session).close(eq(CloseStatus.SERVER_ERROR));
    }
}
