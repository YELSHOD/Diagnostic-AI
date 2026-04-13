package com.yelshod.diagnosticserviceai.ws;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@ExtendWith(MockitoExtension.class)
class WsMessageSenderTest {

    @Mock
    private WebSocketSession session;

    @InjectMocks
    private WsMessageSender sender = new WsMessageSender(new ObjectMapper());

    @Test
    void skipsSendWhenSocketIsAlreadyClosed() throws Exception {
        when(session.isOpen()).thenReturn(false);

        sender.send(session, new WsMessage("LOG_LINE", null, "svc", "payload"));

        verify(session, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void swallowsBrokenPipeWithoutThrowing() throws Exception {
        when(session.isOpen()).thenReturn(true);
        org.mockito.Mockito.doThrow(new IOException("Broken pipe"))
                .when(session)
                .sendMessage(any(TextMessage.class));

        assertThatCode(() -> sender.send(
                session,
                new WsMessage("LOG_LINE", null, "svc", "payload")
        )).doesNotThrowAnyException();
    }
}
