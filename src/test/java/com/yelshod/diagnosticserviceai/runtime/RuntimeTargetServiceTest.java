package com.yelshod.diagnosticserviceai.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RuntimeTargetServiceTest {

    @Test
    void mergesTargetsFromAllDiscoveryProvidersInStableOrder() {
        RuntimeTargetDiscoveryService dockerDiscovery = () -> List.of(
                new RuntimeTargetDto(
                        "docker-orders",
                        "orders",
                        RuntimeTargetType.DOCKER_CONTAINER,
                        RuntimeTargetStatus.UP,
                        "localhost",
                        8081,
                        null,
                        LogSourceType.DOCKER,
                        "docker-orders",
                        Map.of("source", "docker")));
        RuntimeTargetDiscoveryService localDiscovery = () -> List.of(
                new RuntimeTargetDto(
                        "local-api",
                        "api",
                        RuntimeTargetType.LOCAL_SERVICE,
                        RuntimeTargetStatus.UNKNOWN,
                        "127.0.0.1",
                        8080,
                        null,
                        LogSourceType.FILE_TAIL,
                        "/tmp/api.log",
                        Map.of("source", "db")));

        RuntimeTargetService service = new RuntimeTargetService(
                List.of(localDiscovery, dockerDiscovery),
                null,
                Clock.systemUTC());

        assertThat(service.listRuntimeTargets())
                .extracting(RuntimeTargetDto::id)
                .containsExactly("docker-orders", "local-api");
    }

    @Test
    void createsPersistedLocalServiceTarget() {
        RuntimeTargetRepository runtimeTargetRepository = org.mockito.Mockito.mock(RuntimeTargetRepository.class);
        Instant now = Instant.parse("2026-04-11T08:00:00Z");
        RuntimeTargetEntity savedEntity = RuntimeTargetEntity.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .name("diagnostic-ai-front")
                .type(RuntimeTargetType.LOCAL_SERVICE)
                .host("localhost")
                .port(5173)
                .healthUrl("http://localhost:5173/actuator/health")
                .logSourceType(LogSourceType.FILE_TAIL)
                .logSourceRef("/tmp/front.log")
                .enabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        when(runtimeTargetRepository.save(any(RuntimeTargetEntity.class))).thenReturn(savedEntity);
        RuntimeTargetService service = new RuntimeTargetService(
                List.of(),
                runtimeTargetRepository,
                Clock.fixed(now, ZoneOffset.UTC));

        RuntimeTargetDto result = service.createLocalService(new UpsertRuntimeTargetRequest(
                "diagnostic-ai-front",
                "localhost",
                5173,
                "http://localhost:5173/actuator/health",
                LogSourceType.FILE_TAIL,
                "/tmp/front.log",
                true));

        assertThat(result.id()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(result.type()).isEqualTo(RuntimeTargetType.LOCAL_SERVICE);
        assertThat(result.logSourceType()).isEqualTo(LogSourceType.FILE_TAIL);
    }

    @Test
    void updatesExistingLocalServiceTarget() {
        RuntimeTargetRepository runtimeTargetRepository = org.mockito.Mockito.mock(RuntimeTargetRepository.class);
        Instant createdAt = Instant.parse("2026-04-10T08:00:00Z");
        Instant updatedAt = Instant.parse("2026-04-11T08:00:00Z");
        UUID runtimeTargetId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        RuntimeTargetEntity existingEntity = RuntimeTargetEntity.builder()
                .id(runtimeTargetId)
                .name("diagnostic-ai-front")
                .type(RuntimeTargetType.LOCAL_SERVICE)
                .host("localhost")
                .port(5173)
                .healthUrl("http://localhost:5173/actuator/health")
                .logSourceType(LogSourceType.FILE_TAIL)
                .logSourceRef("/tmp/front.log")
                .enabled(true)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
        when(runtimeTargetRepository.findById(runtimeTargetId)).thenReturn(Optional.of(existingEntity));
        when(runtimeTargetRepository.save(existingEntity)).thenReturn(existingEntity);
        RuntimeTargetService service = new RuntimeTargetService(
                List.of(),
                runtimeTargetRepository,
                Clock.fixed(updatedAt, ZoneOffset.UTC));

        RuntimeTargetDto result = service.updateLocalService(runtimeTargetId.toString(), new UpsertRuntimeTargetRequest(
                "diagnostic-ai-front",
                "127.0.0.1",
                5174,
                "http://127.0.0.1:5174/actuator/health",
                LogSourceType.HTTP_INGEST,
                "front-service",
                false));

        assertThat(result.port()).isEqualTo(5174);
        assertThat(result.logSourceType()).isEqualTo(LogSourceType.HTTP_INGEST);
        assertThat(existingEntity.isEnabled()).isFalse();
        assertThat(existingEntity.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void rejectsUpdatingUnknownRuntimeTarget() {
        RuntimeTargetRepository runtimeTargetRepository = org.mockito.Mockito.mock(RuntimeTargetRepository.class);
        when(runtimeTargetRepository.findById(UUID.fromString("11111111-1111-1111-1111-111111111111")))
                .thenReturn(Optional.empty());
        RuntimeTargetService service = new RuntimeTargetService(
                List.of(),
                runtimeTargetRepository,
                Clock.systemUTC());

        assertThatThrownBy(() -> service.updateLocalService(
                        "11111111-1111-1111-1111-111111111111",
                        new UpsertRuntimeTargetRequest(
                                "diagnostic-ai-front",
                                "localhost",
                                5173,
                                "http://localhost:5173/actuator/health",
                                LogSourceType.FILE_TAIL,
                                "/tmp/front.log",
                                true)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Runtime target not found");
    }

    @Test
    void deletesExistingLocalServiceTarget() {
        RuntimeTargetRepository runtimeTargetRepository = org.mockito.Mockito.mock(RuntimeTargetRepository.class);
        UUID runtimeTargetId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        when(runtimeTargetRepository.findById(runtimeTargetId)).thenReturn(Optional.of(
                RuntimeTargetEntity.builder()
                        .id(runtimeTargetId)
                        .name("diagnostic-ai-front")
                        .type(RuntimeTargetType.LOCAL_SERVICE)
                        .host("localhost")
                        .port(5173)
                        .healthUrl("http://localhost:5173/actuator/health")
                        .logSourceType(LogSourceType.FILE_TAIL)
                        .logSourceRef("/tmp/front.log")
                        .enabled(true)
                        .createdAt(Instant.parse("2026-04-10T08:00:00Z"))
                        .updatedAt(Instant.parse("2026-04-10T08:00:00Z"))
                        .build()));
        RuntimeTargetService service = new RuntimeTargetService(
                List.of(),
                runtimeTargetRepository,
                Clock.systemUTC());

        service.deleteLocalService(runtimeTargetId.toString());

        verify(runtimeTargetRepository).deleteById(runtimeTargetId);
    }
}
