package com.yelshod.diagnosticserviceai.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yelshod.diagnosticserviceai.cluster.ClusterResult;
import com.yelshod.diagnosticserviceai.cluster.ClusterService;
import com.yelshod.diagnosticserviceai.logs.ErrorEvent;
import com.yelshod.diagnosticserviceai.persistence.entity.LogEventEntity;
import com.yelshod.diagnosticserviceai.persistence.entity.ProjectEntity;
import com.yelshod.diagnosticserviceai.persistence.repository.LogEventRepository;
import com.yelshod.diagnosticserviceai.persistence.repository.ProjectRepository;
import com.yelshod.diagnosticserviceai.runtime.LogSourceType;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetEntity;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetRepository;
import com.yelshod.diagnosticserviceai.runtime.RuntimeTargetType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PublicLogIngestServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private RuntimeTargetRepository runtimeTargetRepository;

    @Mock
    private LogEventRepository logEventRepository;

    @Mock
    private ClusterService clusterService;

    @Test
    void createsHttpIngestRuntimeTargetAndRecordsErrorIncident() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Diploma Demo")
                .projectKey("prj_demo")
                .createdAt(Instant.parse("2026-05-17T10:00:00Z"))
                .build();
        RuntimeTargetEntity savedTarget = RuntimeTargetEntity.builder()
                .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .projectId(projectId)
                .name("orders-local")
                .type(RuntimeTargetType.LOCAL_SERVICE)
                .logSourceType(LogSourceType.HTTP_INGEST)
                .logSourceRef("project:" + projectId)
                .enabled(true)
                .createdAt(Instant.parse("2026-05-17T10:01:00Z"))
                .updatedAt(Instant.parse("2026-05-17T10:01:00Z"))
                .build();
        when(projectRepository.findByProjectKey("prj_demo")).thenReturn(Optional.of(project));
        when(runtimeTargetRepository.findByProjectIdAndName(projectId, "orders-local")).thenReturn(Optional.empty());
        when(runtimeTargetRepository.save(any(RuntimeTargetEntity.class))).thenReturn(savedTarget);
        when(clusterService.processEvent(any(ErrorEvent.class), any(UUID.class)))
                .thenReturn(new ClusterResult("cluster-1", "orders-local", 1, true));

        PublicLogIngestService service = new PublicLogIngestService(
                projectRepository,
                runtimeTargetRepository,
                logEventRepository,
                clusterService,
                Clock.fixed(Instant.parse("2026-05-17T10:01:00Z"), ZoneOffset.UTC));

        LogIngestResponse response = service.ingest(new LogIngestRequest(
                "prj_demo",
                "orders-local",
                "ERROR",
                "Order failed",
                "java.lang.IllegalStateException: boom\n    at demo.OrderService.create(OrderService.java:42)",
                Instant.parse("2026-05-17T10:00:30Z"),
                "local"));

        assertThat(response.errorRecorded()).isTrue();
        assertThat(response.runtimeTargetId()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(response.clusterKey()).isEqualTo("cluster-1");

        ArgumentCaptor<RuntimeTargetEntity> targetCaptor = ArgumentCaptor.forClass(RuntimeTargetEntity.class);
        verify(runtimeTargetRepository).save(targetCaptor.capture());
        assertThat(targetCaptor.getValue().getProjectId()).isEqualTo(projectId);
        assertThat(targetCaptor.getValue().getLogSourceType()).isEqualTo(LogSourceType.HTTP_INGEST);

        ArgumentCaptor<LogEventEntity> logCaptor = ArgumentCaptor.forClass(LogEventEntity.class);
        verify(logEventRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getProjectId()).isEqualTo(projectId);
        assertThat(logCaptor.getValue().getLevel()).isEqualTo("ERROR");

        ArgumentCaptor<ErrorEvent> eventCaptor = ArgumentCaptor.forClass(ErrorEvent.class);
        verify(clusterService).processEvent(eventCaptor.capture(), any(UUID.class));
        assertThat(eventCaptor.getValue().exceptionType()).isEqualTo("java.lang.IllegalStateException");
        assertThat(eventCaptor.getValue().stackFrames()).contains("java.lang.IllegalStateException: boom");
    }

    @Test
    void acceptsNonErrorLogWithoutRecordingIncident() {
        UUID projectId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ProjectEntity project = ProjectEntity.builder()
                .id(projectId)
                .name("Diploma Demo")
                .projectKey("prj_demo")
                .createdAt(Instant.parse("2026-05-17T10:00:00Z"))
                .build();
        RuntimeTargetEntity target = RuntimeTargetEntity.builder()
                .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                .projectId(projectId)
                .name("orders-local")
                .type(RuntimeTargetType.LOCAL_SERVICE)
                .logSourceType(LogSourceType.HTTP_INGEST)
                .logSourceRef("project:" + projectId)
                .enabled(true)
                .createdAt(Instant.parse("2026-05-17T10:01:00Z"))
                .updatedAt(Instant.parse("2026-05-17T10:01:00Z"))
                .build();
        when(projectRepository.findByProjectKey("prj_demo")).thenReturn(Optional.of(project));
        when(runtimeTargetRepository.findByProjectIdAndName(projectId, "orders-local")).thenReturn(Optional.of(target));

        PublicLogIngestService service = new PublicLogIngestService(
                projectRepository,
                runtimeTargetRepository,
                logEventRepository,
                clusterService,
                Clock.systemUTC());

        LogIngestResponse response = service.ingest(new LogIngestRequest(
                "prj_demo",
                "orders-local",
                "INFO",
                "Order created",
                null,
                null,
                "local"));

        assertThat(response.errorRecorded()).isFalse();
        assertThat(response.clusterKey()).isNull();
        verify(logEventRepository).save(any(LogEventEntity.class));
        verify(clusterService, never()).processEvent(any(), any(UUID.class));
    }
}
