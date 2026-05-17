package com.yelshod.diagnosticserviceai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiDiagnosisService {

    private final DiagnosisPromptFactory diagnosisPromptFactory;
    private final GeminiClient geminiClient;
    private final AiDiagnosisPersistenceService aiDiagnosisPersistenceService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public AiDiagnosisResponse diagnose(AiDiagnosisRequest request) {
        AppProperties.Gemini gemini = appProperties.gemini();
        if (gemini.apiKey() == null || gemini.apiKey().isBlank()) {
            log.warn("AI diagnosis rejected because Gemini is not configured");
            throw new AiDiagnosisDisabledException("Gemini integration is not configured");
        }

        boolean hasQuestion = request.question() != null && !request.question().isBlank();
        boolean hasLogLines = request.logLines() != null && !request.logLines().isEmpty();
        if (!hasQuestion && !hasLogLines) {
            throw new IllegalArgumentException("Question or logLines must be provided");
        }

        log.info("AI diagnosis requested service={} model={}", request.service(), gemini.model());
        try {
            String prompt = diagnosisPromptFactory.buildInputJson(request);
            String rawText = geminiClient.generateDiagnosisJson(gemini.model(), prompt);
            JsonNode parsed = parseDiagnosisJson(rawText);
            List<String> timeline = readStringList(parsed.path("timeline"));
            List<String> evidence = readStringList(parsed.path("evidence"));
            List<String> nextChecks = readStringList(parsed.path("nextChecks"));

            String summary = parsed.path("summary").isTextual() ? parsed.path("summary").asText() : rawText;
            String probableRootCause = parsed.path("probableRootCause").isTextual()
                    ? parsed.path("probableRootCause").asText()
                    : "";
            AiDiagnosisResponse response = new AiDiagnosisResponse(
                    "gemini",
                    gemini.model(),
                    gemini.promptVersion(),
                    summary,
                    timeline,
                    probableRootCause,
                    evidence,
                    nextChecks,
                    rawText
            );
            log.info("AI diagnosis completed service={} model={}", request.service(), gemini.model());
            return response;
        } catch (AiDiagnosisProviderException ex) {
            log.warn("AI diagnosis provider call failed model={} message={}", gemini.model(), ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("AI diagnosis provider call failed model={}", gemini.model(), ex);
            throw new AiDiagnosisProviderException("Gemini diagnosis failed", ex);
        }
    }

    public void diagnoseNewCluster(String clusterKey, ErrorEvent event) {
        AppProperties.Gemini gemini = appProperties.gemini();
        if (gemini.apiKey() == null || gemini.apiKey().isBlank()) {
            return;
        }

        try {
            String prompt = diagnosisPromptFactory.buildInputJson(event);
            String diagnosisJson = geminiClient.generateDiagnosisJson(gemini.model(), prompt);
            aiDiagnosisPersistenceService.save(clusterKey, gemini.model(), gemini.promptVersion(), diagnosisJson);
        } catch (AiDiagnosisProviderException ex) {
            log.warn("Auto Gemini diagnosis skipped clusterKey={} message={}", clusterKey, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Auto Gemini diagnosis failed clusterKey={} message={}", clusterKey, ex.getMessage());
            log.debug("Auto Gemini diagnosis failure details clusterKey={}", clusterKey, ex);
        }
    }

    private List<String> readStringList(JsonNode node) {
        List<String> items = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> items.add(item.asText()));
        }
        return List.copyOf(items);
    }

    private JsonNode parseDiagnosisJson(String rawText) throws JsonProcessingException {
        String normalized = normalizeModelResponse(rawText);
        try {
            return objectMapper.readTree(normalized);
        } catch (JsonProcessingException ex) {
            return objectMapper.valueToTree(new FallbackDiagnosis(rawText));
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
        private FallbackDiagnosis(String rawText) {
            this(rawText, List.of(), "", List.of(), List.of());
        }
    }
}
