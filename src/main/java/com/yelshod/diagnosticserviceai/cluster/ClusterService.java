package com.yelshod.diagnosticserviceai.cluster;

import com.yelshod.diagnosticserviceai.ai.AiDiagnosisService;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import com.yelshod.diagnosticserviceai.persistence.entity.ClusterEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.IncidentEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.ClusterRepository;
import com.yelshod.diagnosticserviceai.persistence.repository.IncidentRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterRepository clusterRepository;
    private final IncidentRepository incidentRepository;
    private final AiDiagnosisService aiDiagnosisService;

    @Transactional
    public ClusterResult processEvent(ErrorEvent event) {
        Instant now = Instant.now();
        String topFrame = event.stackFrames().isEmpty() ? "no-frame" : event.stackFrames().getFirst();
        String key = buildClusterKey(event.exceptionType(), topFrame, event.message());

        ClusterEntity cluster = clusterRepository.findById(key).orElse(null);
        boolean isNew = cluster == null;

        if (isNew) {
            cluster = ClusterEntity.builder()
                    .clusterKey(key)
                    .service(event.service())
                    .title(event.exceptionType())
                    .severity("high")
                    .firstSeen(now)
                    .lastSeen(now)
                    .count(1)
                    .build();
        } else {
            cluster.setCount(cluster.getCount() + 1);
            cluster.setLastSeen(now);
        }
        clusterRepository.save(cluster);

        IncidentEntity incident = IncidentEntity.builder()
                .id(UUID.randomUUID())
                .clusterKey(key)
                .service(event.service())
                .eventTime(event.eventTime())
                .traceId(event.traceId())
                .exceptionType(event.exceptionType())
                .message(event.message())
                .topFrame(topFrame)
                .stacktrace(event.stacktraceFull())
                .context(String.join("\n", event.contextLines()))
                .build();
        incidentRepository.save(incident);

        if (isNew) {
            aiDiagnosisService.diagnoseNewCluster(key, event);
        }
        return new ClusterResult(key, event.service(), cluster.getCount(), isNew);
    }

    private String buildClusterKey(String exceptionType, String topFrame, String message) {
        String normalizedHash = sha256(normalizeMessage(message));
        return exceptionType + "|" + topFrame + "|" + normalizedHash;
    }

    private String normalizeMessage(String message) {
        if (message == null) {
            return "";
        }
        return message
                .toLowerCase()
                .replaceAll("[0-9]+", "#")
                .replaceAll("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}", "{uuid}")
                .replaceAll("\\d{4}-\\d{2}-\\d{2}[ t]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?", "{date}")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
