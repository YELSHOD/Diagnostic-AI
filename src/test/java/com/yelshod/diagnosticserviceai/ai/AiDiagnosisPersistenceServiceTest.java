package com.yelshod.diagnosticserviceai.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.yelshod.diagnosticserviceai.persistence.entity.AiDiagnosisEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.AiDiagnosisRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class AiDiagnosisPersistenceServiceTest {

    private final AiDiagnosisRepository repository = Mockito.mock(AiDiagnosisRepository.class);
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final AiDiagnosisPersistenceService service = new AiDiagnosisPersistenceService(repository, objectMapper);

    @Test
    void stripsMarkdownFenceBeforeSavingJsonbPayload() throws Exception {
        service.save("cluster-1", "gemini", "v1", """
                ```json
                {"summary":"ok","timeline":[],"probableRootCause":"","evidence":[],"nextChecks":[]}
                ```
                """);

        ArgumentCaptor<AiDiagnosisEntity> captor = ArgumentCaptor.forClass(AiDiagnosisEntity.class);
        verify(repository).save(captor.capture());
        JsonNode parsed = objectMapper.readTree(captor.getValue().getDiagnosisJson());
        assertThat(parsed.path("summary").asText()).isEqualTo("ok");
    }

    @Test
    void wrapsPlainTextInValidJsonBeforeSaving() throws Exception {
        service.save("cluster-1", "gemini", "v1", "Plain model response");

        ArgumentCaptor<AiDiagnosisEntity> captor = ArgumentCaptor.forClass(AiDiagnosisEntity.class);
        verify(repository).save(captor.capture());
        JsonNode parsed = objectMapper.readTree(captor.getValue().getDiagnosisJson());
        assertThat(parsed.path("summary").asText()).isEqualTo("Plain model response");
        assertThat(parsed.path("timeline").isArray()).isTrue();
    }

    @Test
    void keepsAlreadyValidJson() throws Exception {
        service.save("cluster-1", "gemini", "v1", "{\"summary\":\"valid\"}");

        ArgumentCaptor<AiDiagnosisEntity> captor = ArgumentCaptor.forClass(AiDiagnosisEntity.class);
        verify(repository).save(captor.capture());
        assertThat(objectMapper.readTree(captor.getValue().getDiagnosisJson()).path("summary").asText())
                .isEqualTo("valid");
        verify(repository).save(any(AiDiagnosisEntity.class));
    }
}
