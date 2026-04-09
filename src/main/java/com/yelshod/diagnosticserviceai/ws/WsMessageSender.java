package com.yelshod.diagnosticserviceai.ws;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Slf4j
@Component
@RequiredArgsConstructor
public class WsMessageSender {

    private final ObjectMapper objectMapper;

    public void send(WebSocketSession session, WsMessage message) {
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
}
