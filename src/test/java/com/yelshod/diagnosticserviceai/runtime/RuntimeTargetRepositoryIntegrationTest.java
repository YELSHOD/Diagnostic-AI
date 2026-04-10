package com.yelshod.diagnosticserviceai.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.yelshod.diagnosticserviceai.support.PostgresIntegrationTest;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RuntimeTargetRepositoryIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private RuntimeTargetRepository runtimeTargetRepository;

    @BeforeEach
    void cleanDatabase() {
        runtimeTargetRepository.deleteAll();
    }

    @Test
    void savesLocalServiceRuntimeTarget() {
        RuntimeTargetEntity entity = RuntimeTargetEntity.builder()
                .id(UUID.randomUUID())
                .name("diagnostic-ai-front")
                .type(RuntimeTargetType.LOCAL_SERVICE)
                .host("localhost")
                .port(5173)
                .healthUrl("http://localhost:5173")
                .logSourceType(LogSourceType.FILE_TAIL)
                .logSourceRef("/tmp/diagnostic-ai-front.log")
                .enabled(true)
                .createdAt(Instant.parse("2026-04-10T08:00:00Z"))
                .updatedAt(Instant.parse("2026-04-10T08:00:00Z"))
                .build();

        RuntimeTargetEntity saved = runtimeTargetRepository.save(entity);

        assertThat(saved.getId()).isNotNull();
        assertThat(runtimeTargetRepository.findAllByEnabledTrueOrderByNameAsc())
                .singleElement()
                .extracting(RuntimeTargetEntity::getName, RuntimeTargetEntity::getType, RuntimeTargetEntity::getLogSourceType)
                .containsExactly("diagnostic-ai-front", RuntimeTargetType.LOCAL_SERVICE, LogSourceType.FILE_TAIL);
    }
}
