package com.yelshod.diagnosticserviceai.ai;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
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
