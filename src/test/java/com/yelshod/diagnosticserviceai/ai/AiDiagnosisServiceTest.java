package com.yelshod.diagnosticserviceai.ai;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.config.AppProperties;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiDiagnosisServiceTest {

    @Mock
    private DiagnosisPromptFactory diagnosisPromptFactory;

    @Mock
    private GeminiClient geminiClient;

    @Mock
    private AiDiagnosisPersistenceService aiDiagnosisPersistenceService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private AiDiagnosisService aiDiagnosisService;

    @Test
    void skipsRemoteCallWhenApiKeyIsBlank() {
        when(appProperties.gemini()).thenReturn(new AppProperties.Gemini("", "gemini-2.5-flash", "v1"));

        aiDiagnosisService.diagnoseNewCluster("cluster-1", new ErrorEvent(
                "orders", Instant.now(), "trace", "IllegalStateException", "boom", List.of(), "stack", List.of()));

        verify(geminiClient, never()).generateDiagnosisJson("gemini-2.5-flash", "");
    }
}
