package com.yelshod.diagnosticserviceai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            JsonNode parsed = objectMapper.readTree(rawText);
            List<String> bullets = new ArrayList<>();
            if (parsed.path("bullets").isArray()) {
                parsed.path("bullets").forEach(node -> bullets.add(node.asText()));
            }

            String summary = parsed.path("summary").isTextual() ? parsed.path("summary").asText() : rawText;
            AiDiagnosisResponse response = new AiDiagnosisResponse(
                    "gemini",
                    gemini.model(),
                    gemini.promptVersion(),
                    summary,
                    List.copyOf(bullets),
                    rawText
            );
            log.info("AI diagnosis completed service={} model={}", request.service(), gemini.model());
            return response;
        } catch (AiDiagnosisProviderException ex) {
            log.error("AI diagnosis provider call failed model={}", gemini.model(), ex);
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
        } catch (Exception ex) {
            log.warn("Failed to get Gemini diagnosis for cluster {}", clusterKey, ex);
        }
    }
}
