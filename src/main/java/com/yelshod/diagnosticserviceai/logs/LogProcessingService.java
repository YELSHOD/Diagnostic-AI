package com.yelshod.diagnosticserviceai.logs;

import com.yelshod.diagnosticserviceai.cluster.ClusterResult;
import com.yelshod.diagnosticserviceai.cluster.ClusterService;
import com.yelshod.diagnosticserviceai.common.RedactionService;
import com.yelshod.diagnosticserviceai.docker.DockerLogLine;
import com.yelshod.diagnosticserviceai.ws.WsMessage;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogProcessingService {

    private final LogParser logParser;
    private final EventAssembler eventAssembler;
    private final ClusterService clusterService;
    private final RedactionService redactionService;

    public WsMessage toLogMessage(DockerLogLine line) {
        ParsedLogLine parsed = logParser.parse(line.service(), line.line());
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", redactionService.redact(parsed.message()));
        payload.put("level", parsed.level());
        payload.put("traceId", parsed.traceId());
        return new WsMessage("LOG_LINE", parsed.timestamp() == null ? Instant.now() : parsed.timestamp(), parsed.service(),
                payload);
    }

    public Optional<ErrorEvent> maybeBuildErrorEvent(DockerLogLine line, Consumer<ErrorEvent> timeoutConsumer) {
        ParsedLogLine parsed = logParser.parse(line.service(), line.line());
        return eventAssembler.process(parsed, timeoutConsumer);
    }

    public WsMessage toErrorMessage(ErrorEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("service", event.service());
        payload.put("eventTime", event.eventTime());
        payload.put("traceId", event.traceId());
        payload.put("exceptionType", event.exceptionType());
        payload.put("message", redactionService.redact(event.message()));
        payload.put("topFrames", event.stackFrames());
        payload.put("stacktrace", redactionService.redact(event.stacktraceFull()));
        payload.put("context", event.contextLines().stream().map(redactionService::redact).toList());
        return new WsMessage(
                "ERROR_EVENT",
                event.eventTime(),
                event.service(),
                payload
        );
    }

    public WsMessage toClusterUpdate(ErrorEvent event) {
        ClusterResult result = clusterService.processEvent(event);
        return new WsMessage(
                "CLUSTER_UPDATE",
                Instant.now(),
                result.service(),
                Map.of(
                        "clusterKey", result.clusterKey(),
                        "count", result.count(),
                        "newCluster", result.newCluster()
                )
        );
    }
}
