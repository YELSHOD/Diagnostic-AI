package com.yelshod.diagnosticserviceai.cluster;

import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import com.yelshod.diagnosticserviceai.persistence.entity.IncidentEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.IncidentRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class IncidentRecorder {

    private final IncidentRepository incidentRepository;

    public void record(String clusterKey, ErrorEvent event) {
        record(clusterKey, event, null);
    }

    public void record(String clusterKey, ErrorEvent event, UUID projectId) {
        String topFrame = event.stackFrames().isEmpty() ? "no-frame" : event.stackFrames().getFirst();

        incidentRepository.save(IncidentEntity.builder()
                .id(UUID.randomUUID())
                .projectId(projectId)
                .clusterKey(clusterKey)
                .service(event.service())
                .eventTime(event.eventTime())
                .traceId(event.traceId())
                .exceptionType(event.exceptionType())
                .message(event.message())
                .topFrame(topFrame)
                .stacktrace(event.stacktraceFull())
                .context(String.join("\n", event.contextLines()))
                .build());
    }
}
