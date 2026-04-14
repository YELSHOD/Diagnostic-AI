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

        assertThatThrownBy(() -> service.diagnose(new AiDiagnosisRequest(
                "svc",
                "why?",
                List.of("line"),
                new AiDiagnosisRequest.TimeRange("all", "Showing: All streamed", null, null),
                "",
                "")))
                .isInstanceOf(AiDiagnosisDisabledException.class)
                .hasMessage("Gemini integration is not configured");
    }

    @Test
    void mapsGeminiTextIntoStructuredResponse() {
        when(geminiClient.generateDiagnosisJson(anyString(), anyString()))
                .thenReturn("""
                        {
                          "summary":"Likely root cause",
                          "timeline":["11:20 payment started","11:21 websocket failed"],
                          "probableRootCause":"Expired JWT during reconnect",
                          "evidence":["Observation A","Observation B"],
                          "nextChecks":["Check JWT expiry","Check reconnect handling"]
                        }
                        """);

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

        AiDiagnosisResponse response = service.diagnose(new AiDiagnosisRequest(
                "svc",
                "why?",
                List.of("line"),
                new AiDiagnosisRequest.TimeRange("relative", "Showing: 15m", Instant.parse("2026-04-13T11:06:00Z"), Instant.parse("2026-04-13T11:21:00Z")),
                "ERROR",
                "jwt"));

        assertThat(response.provider()).isEqualTo("gemini");
        assertThat(response.model()).isEqualTo("gemini-2.5-flash");
        assertThat(response.promptVersion()).isEqualTo("v1");
        assertThat(response.summary()).isEqualTo("Likely root cause");
        assertThat(response.timeline()).containsExactly("11:20 payment started", "11:21 websocket failed");
        assertThat(response.probableRootCause()).isEqualTo("Expired JWT during reconnect");
        assertThat(response.evidence()).containsExactly("Observation A", "Observation B");
        assertThat(response.nextChecks()).containsExactly("Check JWT expiry", "Check reconnect handling");
        assertThat(response.rawText()).contains("Likely root cause");
    }

    @Test
    void stripsMarkdownJsonFencesBeforeParsing() {
        when(geminiClient.generateDiagnosisJson(anyString(), anyString()))
                .thenReturn("""
                        ```json
                        {
                          "summary":"Likely root cause",
                          "timeline":["11:20 payment started"],
                          "probableRootCause":"Expired JWT during reconnect",
                          "evidence":["Observation A"],
                          "nextChecks":["Check JWT expiry"]
                        }
                        ```
                        """);

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

        AiDiagnosisResponse response = service.diagnose(new AiDiagnosisRequest(
                "svc",
                "why?",
                List.of("line"),
                new AiDiagnosisRequest.TimeRange("all", "Showing: All streamed", null, null),
                "",
                ""));

        assertThat(response.summary()).isEqualTo("Likely root cause");
        assertThat(response.timeline()).containsExactly("11:20 payment started");
        assertThat(response.probableRootCause()).isEqualTo("Expired JWT during reconnect");
    }

    @Test
    void fallsBackToRawTextWhenGeminiReturnsPlainTextInsteadOfJson() {
        when(geminiClient.generateDiagnosisJson(anyString(), anyString()))
                .thenReturn("Likely root cause is an expired JWT during reconnect.");

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

        AiDiagnosisResponse response = service.diagnose(new AiDiagnosisRequest(
                "svc",
                "why?",
                List.of("line"),
                new AiDiagnosisRequest.TimeRange("all", "Showing: All streamed", null, null),
                "",
                ""));

        assertThat(response.summary()).isEqualTo("Likely root cause is an expired JWT during reconnect.");
        assertThat(response.timeline()).isEmpty();
        assertThat(response.evidence()).isEmpty();
        assertThat(response.nextChecks()).isEmpty();
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

        assertThatThrownBy(() -> service.diagnose(new AiDiagnosisRequest(
                "svc",
                "why?",
                List.of("line"),
                new AiDiagnosisRequest.TimeRange("all", "Showing: All streamed", null, null),
                "",
                "")))
                .isInstanceOf(AiDiagnosisProviderException.class)
                .hasMessage("Gemini request failed");
    }
}
