package com.yelshod.diagnosticserviceai.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yelshod.diagnosticserviceai.persistence.entity.AiDiagnosisEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.AiDiagnosisRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiDiagnosisPersistenceService {

    private final AiDiagnosisRepository aiDiagnosisRepository;
    private final ObjectMapper objectMapper;

    public void save(String clusterKey, String model, String promptVersion, String diagnosisJson) {
        aiDiagnosisRepository.save(AiDiagnosisEntity.builder()
                .clusterKey(clusterKey)
                .model(model)
                .promptVersion(promptVersion)
                .diagnosisJson(toValidJson(diagnosisJson))
                .createdAt(Instant.now())
                .build());
    }

    private String toValidJson(String rawText) {
        String normalized = normalizeModelResponse(rawText);
        try {
            objectMapper.readTree(normalized);
            return normalized;
        } catch (JsonProcessingException ignored) {
            try {
                return objectMapper.writeValueAsString(new FallbackDiagnosis(normalized));
            } catch (JsonProcessingException ex) {
                throw new IllegalStateException("Unable to serialize fallback AI diagnosis", ex);
            }
        }
    }

    private String normalizeModelResponse(String rawText) {
        String normalized = rawText == null ? "" : rawText.trim();
        if (normalized.startsWith("```")) {
            int firstNewline = normalized.indexOf('\n');
            if (firstNewline >= 0) {
                normalized = normalized.substring(firstNewline + 1).trim();
            }
            if (normalized.endsWith("```")) {
                normalized = normalized.substring(0, normalized.length() - 3).trim();
            }
        }
        return normalized;
    }

    private record FallbackDiagnosis(
            String summary,
            List<String> timeline,
            String probableRootCause,
            List<String> evidence,
            List<String> nextChecks
    ) {
        private FallbackDiagnosis(String summary) {
            this(summary, List.of(), "", List.of(), List.of());
        }
    }
}
