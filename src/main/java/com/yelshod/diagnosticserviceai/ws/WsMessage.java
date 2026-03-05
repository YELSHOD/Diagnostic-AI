package com.yelshod.diagnosticserviceai.ws;

import java.time.Instant;

public record WsMessage(
        String type,
        Instant ts,
        String service,
        Object payload
) {
}
