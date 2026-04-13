package com.yelshod.diagnosticserviceai.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yelshod.diagnosticserviceai.common.RedactionService;
import com.yelshod.diagnosticserviceai.config.AppProperties;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiDiagnosisServiceTest {

    private final DiagnosisPromptFactory promptFactory =
            new DiagnosisPromptFactory(new RedactionService(), JsonMapper.builder().findAndAddModules().build());
    private final GeminiClient geminiClient = mock(GeminiClient.class);
    private final AiDiagnosisPersistenceService persistenceService = mock(AiDiagnosisPersistenceService.class);

    @Test
    void throwsWhenGeminiIsNotConfigured() {
        AiDiagnosisService service = new AiDiagnosisService(
                promptFactory,
                geminiClient,
                persistenceService,
                new AppProperties(
                        new AppProperties.Docker("label", "value", 100),
                        new AppProperties.Gemini("", "gemini-2.5-flash", "v1"),
                        new AppProperties.Runtime(List.of()),
                        new AppProperties.Demo(false, false, 0, "", "")
                ),
                JsonMapper.builder().findAndAddModules().build()
        );

        assertThatThrownBy(() -> service.diagnose(new AiDiagnosisRequest("svc", "why?", List.of("line"))))
                .isInstanceOf(AiDiagnosisDisabledException.class)
                .hasMessage("Gemini integration is not configured");
    }

    @Test
    void mapsGeminiTextIntoStructuredResponse() {
        when(geminiClient.generateDiagnosisJson(anyString(), anyString()))
                .thenReturn("{\"summary\":\"Likely root cause\",\"bullets\":[\"Observation A\",\"Observation B\"]}");

        AiDiagnosisService service = new AiDiagnosisService(
                promptFactory,
                geminiClient,
                persistenceService,
                new AppProperties(
                        new AppProperties.Docker("label", "value", 100),
                        new AppProperties.Gemini("secret", "gemini-2.5-flash", "v1"),
                        new AppProperties.Runtime(List.of()),
                        new AppProperties.Demo(false, false, 0, "", "")
                ),
                JsonMapper.builder().findAndAddModules().build()
        );

        AiDiagnosisResponse response = service.diagnose(new AiDiagnosisRequest("svc", "why?", List.of("line")));

        assertThat(response.provider()).isEqualTo("gemini");
        assertThat(response.model()).isEqualTo("gemini-2.5-flash");
        assertThat(response.promptVersion()).isEqualTo("v1");
        assertThat(response.summary()).isEqualTo("Likely root cause");
        assertThat(response.bullets()).containsExactly("Observation A", "Observation B");
        assertThat(response.rawText()).contains("Likely root cause");
    }

    @Test
    void skipsRemoteCallForClusterDiagnosisWhenApiKeyIsBlank() {
        AiDiagnosisService service = new AiDiagnosisService(
                promptFactory,
                geminiClient,
                persistenceService,
                new AppProperties(
                        new AppProperties.Docker("label", "value", 100),
                        new AppProperties.Gemini("", "gemini-2.5-flash", "v1"),
                        new AppProperties.Runtime(List.of()),
                        new AppProperties.Demo(false, false, 0, "", "")
                ),
                JsonMapper.builder().findAndAddModules().build()
        );

        service.diagnoseNewCluster("cluster-1", new ErrorEvent(
                "orders", Instant.now(), "trace", "IllegalStateException", "boom", List.of(), "stack", List.of()));

        verify(geminiClient, org.mockito.Mockito.never()).generateDiagnosisJson(anyString(), anyString());
    }

    @Test
    void wrapsProviderFailureForSynchronousDiagnosis() {
        when(geminiClient.generateDiagnosisJson(anyString(), anyString()))
                .thenThrow(new AiDiagnosisProviderException("Gemini request failed", new RuntimeException("boom")));

        AiDiagnosisService service = new AiDiagnosisService(
                promptFactory,
                geminiClient,
                persistenceService,
                new AppProperties(
                        new AppProperties.Docker("label", "value", 100),
                        new AppProperties.Gemini("secret", "gemini-2.5-flash", "v1"),
                        new AppProperties.Runtime(List.of()),
                        new AppProperties.Demo(false, false, 0, "", "")
                ),
                JsonMapper.builder().findAndAddModules().build()
        );

        assertThatThrownBy(() -> service.diagnose(new AiDiagnosisRequest("svc", "why?", List.of("line"))))
                .isInstanceOf(AiDiagnosisProviderException.class)
                .hasMessage("Gemini request failed");
    }
}
