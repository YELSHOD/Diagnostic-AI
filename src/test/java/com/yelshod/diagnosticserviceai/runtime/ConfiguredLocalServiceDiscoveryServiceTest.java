package com.yelshod.diagnosticserviceai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfiguredLocalServiceDiscoveryServiceTest {

    @Test
    void mapsEnabledLocalServicesIntoRuntimeTargets() {
        RuntimeTargetRepository repository = mock(RuntimeTargetRepository.class);
        RuntimeStatusProbe runtimeStatusProbe = mock(RuntimeStatusProbe.class);
        RuntimeTargetEntity entity = RuntimeTargetEntity.builder()
                .id(UUID.fromString("00000000-0000-0000-0000-000000000111"))
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
        when(repository.findAllByEnabledTrueOrderByNameAsc()).thenReturn(List.of(entity));
        when(runtimeStatusProbe.probe("http://localhost:5173")).thenReturn(RuntimeTargetStatus.UP);

        ConfiguredLocalServiceDiscoveryService service = new ConfiguredLocalServiceDiscoveryService(repository, runtimeStatusProbe);

        assertThat(service.discover())
                .singleElement()
                .satisfies(target -> {
                    assertThat(target.id()).isEqualTo("00000000-0000-0000-0000-000000000111");
                    assertThat(target.type()).isEqualTo(RuntimeTargetType.LOCAL_SERVICE);
                    assertThat(target.status()).isEqualTo(RuntimeTargetStatus.UP);
                    assertThat(target.logSourceType()).isEqualTo(LogSourceType.FILE_TAIL);
                    assertThat(target.logSourceRef()).isEqualTo("/tmp/diagnostic-ai-front.log");
                });
    }
}
